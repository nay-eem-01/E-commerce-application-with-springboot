package com.ecommerce.payment.service;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.event.PaymentExpiredEvent;
import com.ecommerce.common.event.PaymentSucceededEvent;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.money.Money;
import com.ecommerce.payment.api.OnlinePaymentRequest;
import com.ecommerce.payment.api.PaymentAction;
import com.ecommerce.payment.api.PaymentApi;
import com.ecommerce.payment.api.PaymentMethod;
import com.ecommerce.payment.api.PaymentStatus;
import com.ecommerce.payment.config.PaymentProperties;
import com.ecommerce.payment.config.PaymentRouting;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentEvent;
import com.ecommerce.payment.repository.PaymentEventRepository;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.spi.PaymentProvider;
import com.ecommerce.payment.spi.PaymentProvider.Outcome;
import com.ecommerce.payment.spi.PaymentProvider.Verification;
import com.ecommerce.payment.spi.ProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Payment lifecycle, independent of any vendor. Rules:
 * - A payment becomes PAID only from a provider's server-side answer whose amount and
 *   currency match ours. Browser redirect parameters are never trusted.
 * - Vendor HTTP calls happen outside database transactions (startOnline, callbacks, expiry),
 *   except a refund during an order cancellation, which must roll the cancel back if it fails.
 * - Every step is recorded in payment_events.
 */
@Slf4j
@Service
public class PaymentService implements PaymentApi {

	static final String COD_PROVIDER = "cod";

	private final PaymentRepository paymentRepository;
	private final PaymentEventRepository eventRepository;
	private final PaymentRouting routing;
	private final PaymentProperties properties;
	private final ApplicationEventPublisher events;
	private final Clock clock;
	private final TransactionTemplate newTx;

	/** Hand-written: builds the REQUIRES_NEW template used around vendor calls. */
	public PaymentService(PaymentRepository paymentRepository, PaymentEventRepository eventRepository,
						  PaymentRouting routing, PaymentProperties properties, ApplicationEventPublisher events,
						  Clock clock, PlatformTransactionManager transactionManager) {
		this.paymentRepository = paymentRepository;
		this.eventRepository = eventRepository;
		this.routing = routing;
		this.properties = properties;
		this.events = events;
		this.clock = clock;
		this.newTx = new TransactionTemplate(transactionManager);
		this.newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public record CallbackResult(String orderNumber, PaymentStatus status) {}

	// ---------------------------------------------------------------- PaymentApi

	@Override
	public List<PaymentMethod> availableMethods() {
		return routing.available();
	}

	@Override
	public boolean isAvailable(PaymentMethod method) {
		return routing.isAvailable(method);
	}

	@Override
	@Transactional
	public void createCashOnDelivery(Long orderId, String orderNumber, BigDecimal amount) {
		Payment p = newPayment(orderId, orderNumber, PaymentMethod.COD, COD_PROVIDER, amount, null);
		paymentRepository.save(p);
		record(p.getId(), "CREATED", "cash on delivery");
	}

	@Override
	public PaymentAction startOnline(OnlinePaymentRequest r) {
		if (!r.method().isOnline() || !routing.isAvailable(r.method())) {
			throw ResponseException.badRequest(ErrorCode.PAYMENT_METHOD_UNAVAILABLE);
		}
		PaymentProvider provider = routing.providerFor(r.method());
		Instant now = clock.instant();

		Payment created = newTx.execute(status -> {
			// A retry supersedes the order's earlier unfinished attempt.
			paymentRepository.findByOrderIdOrderByIdDesc(r.orderId()).stream()
					.filter(p -> p.getStatus() == PaymentStatus.PENDING || p.getStatus() == PaymentStatus.FAILED)
					.forEach(p -> {
						p.setStatus(PaymentStatus.CANCELLED);
						record(p.getId(), "SUPERSEDED", "a new attempt was started");
					});
			Payment p = newPayment(r.orderId(), r.orderNumber(), r.method(), provider.id(), r.amount(),
					now.plus(properties.pendingTtl()));
			paymentRepository.save(p);
			record(p.getId(), "CREATED", r.method() + " via " + provider.id());
			return p;
		});

		String base = properties.callbackBaseUrl() + "/api/payments/" + provider.id();
		PaymentProvider.Initiation initiation;
		try {
			initiation = provider.initiate(new PaymentProvider.InitiationRequest(
					created.getReference(), r.orderNumber(), r.method(), Money.of(r.amount()), r.customerName(),
					r.customerPhone(), r.customerEmail(), r.district(), r.addressLine(), r.itemCount(),
					base + "/callback", base + "/ipn"));
		} catch (ProviderException e) {
			log.warn("Provider {} failed to initiate payment {}: {}", provider.id(), created.getReference(), e.getMessage());
			newTx.executeWithoutResult(s -> paymentRepository.findById(created.getId()).ifPresent(p -> {
				p.setStatus(PaymentStatus.FAILED);
				p.setFailureReason("initiation failed");
				record(p.getId(), "INITIATION_FAILED", truncate(e.getMessage()));
			}));
			throw new ResponseException(HttpStatus.BAD_GATEWAY, ErrorCode.PAYMENT_PROVIDER_ERROR);
		}

		newTx.executeWithoutResult(s -> paymentRepository.findById(created.getId()).ifPresent(p -> {
			p.setProviderPaymentId(initiation.providerPaymentId());
			record(p.getId(), "INITIATED", "redirecting shopper to " + provider.id());
		}));
		log.info("Payment {} for order {} started with {}", created.getReference(), r.orderNumber(), provider.id());
		return PaymentAction.redirect(initiation.redirectUrl());
	}

	@Override
	@Transactional(readOnly = true)
	public PaymentStatus statusOf(Long orderId) {
		return paymentRepository.findFirstByOrderIdOrderByIdDesc(orderId).map(Payment::getStatus).orElse(PaymentStatus.PENDING);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isPaid(Long orderId) {
		return statusOf(orderId) == PaymentStatus.PAID;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean hasAttempt(Long orderId) {
		return paymentRepository.findFirstByOrderIdOrderByIdDesc(orderId).isPresent();
	}

	@Override
	@Transactional
	public void markCashCollected(Long orderId) {
		paymentRepository.findFirstByOrderIdOrderByIdDesc(orderId)
				.filter(p -> p.getMethod() == PaymentMethod.COD && p.getStatus() == PaymentStatus.PENDING)
				.ifPresent(p -> {
					p.setStatus(PaymentStatus.PAID);
					p.setPaidAt(clock.instant());
					record(p.getId(), "COLLECTED", "cash collected on delivery");
					events.publishEvent(new PaymentSucceededEvent(orderId, p.getOrderNumber(), p.getReference(), p.getMethod().name(), p.getAmount()));
				});
	}

	@Override
	@Transactional
	public void settleCancellation(Long orderId) {
		for (Payment p : paymentRepository.findByOrderIdOrderByIdDesc(orderId)) {
			switch (p.getStatus()) {
				case PENDING, FAILED -> {
					p.setStatus(PaymentStatus.CANCELLED);
					record(p.getId(), "CANCELLED", "order cancelled");
				}
				case PAID -> {
					if (p.getMethod().isOnline()) {
						refund(p, "Order cancelled");
					}
				}
				default -> { }
			}
		}
	}

	// ---------------------------------------------------------------- callbacks and IPN

	/**
	 * A browser return or a vendor IPN. Finds the payment, asks the vendor's server what really
	 * happened, then applies it. Safe to call any number of times for the same payment.
	 */
	public CallbackResult handleCallback(String providerId, Map<String, String> params, String source) {
		PaymentProvider provider = routing.byId(providerId)
				.orElseThrow(() -> ResponseException.notFound(ErrorCode.PAYMENT_NOT_FOUND));
		PaymentProvider.CallbackKey key = provider.keyOf(params);
		Payment payment = newTx.execute(s -> find(providerId, key));

		if (payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.REFUNDED) {
			return new CallbackResult(payment.getOrderNumber(), payment.getStatus());
		}
		Verification verification;
		try {
			verification = provider.verify(ref(payment), params);
		} catch (ProviderException e) {
			log.warn("Provider {} could not verify payment {} ({}): {}", providerId, payment.getReference(), source, e.getMessage());
			newTx.executeWithoutResult(s -> record(payment.getId(), source, "verification failed, still pending"));
			return new CallbackResult(payment.getOrderNumber(), PaymentStatus.PENDING);
		}
		PaymentStatus status = newTx.execute(s -> apply(payment.getId(), verification, source));
		return new CallbackResult(payment.getOrderNumber(), status);
	}

	// ---------------------------------------------------------------- expiry

	/**
	 * Every minute: online payments past their deadline are checked with the vendor once more
	 * (a late payment still counts) and otherwise expired, which cancels the order.
	 * Runs on every instance; with several instances add a lock (e.g. ShedLock) first.
	 */
	@Scheduled(fixedDelayString = "${app.payment.expiry-check-interval:PT1M}", initialDelayString = "PT30S")
	public void expireOverdue() {
		List<Payment> overdue = newTx.execute(s ->
				paymentRepository.findExpiring(PaymentStatus.PENDING, clock.instant(), PageRequest.of(0, 50)));
		for (Payment p : overdue) {
			try {
				Verification v = routing.byId(p.getProvider()).map(provider -> provider.query(ref(p)))
						.orElse(Verification.pending("unknown provider"));
				if (v.outcome() == Outcome.PAID) {
					newTx.executeWithoutResult(s -> apply(p.getId(), v, "EXPIRY_CHECK"));
					continue;
				}
			} catch (ProviderException e) {
				log.warn("Provider {} unreachable while expiring {}; expiring anyway", p.getProvider(), p.getReference());
			}
			newTx.executeWithoutResult(s -> paymentRepository.findById(p.getId())
					.filter(fresh -> fresh.getStatus() == PaymentStatus.PENDING)
					.ifPresent(fresh -> {
						fresh.setStatus(PaymentStatus.EXPIRED);
						fresh.setFailureReason("not completed in time");
						record(fresh.getId(), "EXPIRED", "not completed within " + properties.pendingTtl());
						events.publishEvent(new PaymentExpiredEvent(fresh.getOrderId(), fresh.getOrderNumber(), fresh.getReference()));
					}));
		}
	}

	// ---------------------------------------------------------------- internals

	/** Runs inside a transaction. Returns the resulting status. */
	PaymentStatus apply(Long paymentId, Verification v, String source) {
		Payment p = paymentRepository.findById(paymentId).orElseThrow();
		record(p.getId(), source, v.outcome() + (v.detail() == null ? "" : ": " + truncate(v.detail())));
		if (p.getStatus() == PaymentStatus.PAID || p.getStatus() == PaymentStatus.REFUNDED) {
			return p.getStatus();
		}
		switch (v.outcome()) {
			case PAID -> {
				boolean amountOk = v.amount() != null && Money.of(v.amount()).compareTo(Money.of(p.getAmount())) == 0;
				boolean currencyOk = v.currency() == null || p.getCurrency().equalsIgnoreCase(v.currency());
				if (!amountOk || !currencyOk) {
					log.warn("Payment {} verified with amount {} {} but expected {} {}; not marking paid",
							p.getReference(), v.amount(), v.currency(), p.getAmount(), p.getCurrency());
					p.setStatus(PaymentStatus.FAILED);
					p.setFailureReason("amount or currency mismatch");
					return p.getStatus();
				}
				PaymentStatus before = p.getStatus();
				p.setStatus(PaymentStatus.PAID);
				p.setPaidAt(clock.instant());
				p.setProviderTransactionId(v.providerTransactionId());
				if (before == PaymentStatus.EXPIRED || before == PaymentStatus.CANCELLED) {
					// Money arrived after the order was given up: give it back automatically.
					log.warn("Payment {} completed after it was {}; refunding", p.getReference(), before);
					refund(p, "Payment completed after the order was cancelled");
					return p.getStatus();
				}
				log.info("Payment {} for order {} PAID via {}", p.getReference(), p.getOrderNumber(), p.getProvider());
				events.publishEvent(new PaymentSucceededEvent(p.getOrderId(), p.getOrderNumber(), p.getReference(), p.getMethod().name(), p.getAmount()));
			}
			case FAILED, CANCELLED -> {
				if (p.getStatus() == PaymentStatus.PENDING) {
					p.setStatus(v.outcome() == Outcome.FAILED ? PaymentStatus.FAILED : PaymentStatus.CANCELLED);
					p.setFailureReason(truncate(v.detail()));
				}
			}
			case PENDING -> { }
		}
		return p.getStatus();
	}

	private void refund(Payment p, String reason) {
		PaymentProvider provider = routing.byId(p.getProvider())
				.orElseThrow(() -> new ResponseException(HttpStatus.BAD_GATEWAY, ErrorCode.PAYMENT_PROVIDER_ERROR));
		PaymentProvider.Refund result;
		try {
			result = provider.refund(ref(p), reason);
		} catch (ProviderException e) {
			log.error("Refund of payment {} via {} failed: {}", p.getReference(), p.getProvider(), e.getMessage());
			throw new ResponseException(HttpStatus.BAD_GATEWAY, ErrorCode.PAYMENT_PROVIDER_ERROR);
		}
		if (!result.succeeded()) {
			log.error("Refund of payment {} via {} refused: {}", p.getReference(), p.getProvider(), result.detail());
			throw new ResponseException(HttpStatus.BAD_GATEWAY, ErrorCode.PAYMENT_PROVIDER_ERROR);
		}
		p.setStatus(PaymentStatus.REFUNDED);
		p.setRefundedAt(clock.instant());
		record(p.getId(), "REFUNDED", "refund " + result.providerRefundId() + ": " + reason);
	}

	private Payment find(String providerId, PaymentProvider.CallbackKey key) {
		Payment p = null;
		if (key.reference() != null) {
			p = paymentRepository.findByReference(key.reference()).orElse(null);
		}
		if (p == null && key.providerPaymentId() != null) {
			p = paymentRepository.findByProviderAndProviderPaymentId(providerId, key.providerPaymentId()).orElse(null);
		}
		if (p == null || !p.getProvider().equals(providerId)) {
			throw ResponseException.notFound(ErrorCode.PAYMENT_NOT_FOUND);
		}
		return p;
	}

	private Payment newPayment(Long orderId, String orderNumber, PaymentMethod method, String provider, BigDecimal amount, Instant expiresAt) {
		Payment p = new Payment();
		p.setOrderId(orderId);
		p.setOrderNumber(orderNumber);
		p.setReference(PaymentReferences.next());
		p.setMethod(method);
		p.setProvider(provider);
		p.setStatus(PaymentStatus.PENDING);
		p.setAmount(Money.of(amount));
		p.setExpiresAt(expiresAt);
		return p;
	}

	private static PaymentProvider.PaymentRef ref(Payment p) {
		return new PaymentProvider.PaymentRef(p.getReference(), p.getProviderPaymentId(), p.getProviderTransactionId(), p.getAmount(), p.getCurrency());
	}

	private void record(Long paymentId, String kind, String detail) {
		PaymentEvent e = new PaymentEvent();
		e.setPaymentId(paymentId);
		e.setKind(kind);
		e.setDetail(truncate(detail));
		e.setCreatedAt(clock.instant());
		eventRepository.save(e);
	}

	private static String truncate(String s) {
		return s == null ? null : (s.length() > 300 ? s.substring(0, 300) : s);
	}
}
