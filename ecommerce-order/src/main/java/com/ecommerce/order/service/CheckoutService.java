package com.ecommerce.order.service;

import com.ecommerce.catalog.api.CatalogApi;
import com.ecommerce.catalog.api.LineRequest;
import com.ecommerce.catalog.api.PricedLine;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.event.OrderPlacedEvent;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.logging.LogMasks;
import com.ecommerce.common.money.Money;
import com.ecommerce.common.phone.PhoneNumbers;
import com.ecommerce.common.ratelimit.RateLimiter;
import com.ecommerce.order.config.DeliveryProperties;
import com.ecommerce.order.config.PaymentProperties;
import com.ecommerce.order.dto.request.CheckoutLine;
import com.ecommerce.order.dto.request.PlaceOrderRequest;
import com.ecommerce.order.dto.request.QuoteRequest;
import com.ecommerce.order.dto.response.DeliveryOptionsResponse;
import com.ecommerce.order.dto.response.OrderResponse;
import com.ecommerce.order.dto.response.QuoteResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatusHistory;
import com.ecommerce.order.entity.Payment;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.enums.PaymentMethod;
import com.ecommerce.order.enums.PaymentStatus;
import com.ecommerce.order.payment.PaymentGateway;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OrderStatusHistoryRepository;
import com.ecommerce.order.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Quote and place orders. Every amount is computed here from catalog prices; the request
 * carries ids and quantities only. Placing an order is one transaction: stock, order, items,
 * history, payment and the OrderPlaced listeners commit together or not at all.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

	private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("^[A-Za-z0-9-]{16,64}$");
	private static final int MAX_LINES = 30;

	private final CatalogApi catalogApi;
	private final DeliveryFeeCalculator feeCalculator;
	private final DeliveryProperties deliveryProperties;
	private final PaymentProperties paymentProperties;
	private final List<PaymentGateway> gateways;
	private final OrderNumberGenerator orderNumberGenerator;
	private final OrderRepository orderRepository;
	private final OrderStatusHistoryRepository historyRepository;
	private final PaymentRepository paymentRepository;
	private final ApplicationEventPublisher events;
	private final RateLimiter rateLimiter;
	private final Clock clock;

	public DeliveryOptionsResponse deliveryOptions() {
		return new DeliveryOptionsResponse(
				deliveryProperties.districts(), deliveryProperties.insideDistricts(),
				Money.of(deliveryProperties.insideFee()), Money.of(deliveryProperties.outsideFee()),
				Money.of(deliveryProperties.freeThreshold()),
				deliveryProperties.insideEstimate(), deliveryProperties.outsideEstimate(),
				paymentProperties.enabledMethods().stream().sorted().toList());
	}

	@Transactional(readOnly = true)
	public QuoteResponse quote(QuoteRequest request) {
		String district = request.getDistrict() == null || request.getDistrict().isBlank()
				? deliveryProperties.insideDistricts().getFirst()
				: feeCalculator.requireKnown(request.getDistrict());
		return buildQuote(catalogApi.price(merge(request.getItems())), district);
	}

	@Transactional
	public OrderResponse place(PlaceOrderRequest request, String idempotencyKey, Long userId, String ip) {
		if (idempotencyKey == null || !IDEMPOTENCY_KEY.matcher(idempotencyKey).matches()) {
			throw ResponseException.badRequest(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
		}
		String phone = PhoneNumbers.normalize(request.getPhone())
				.orElseThrow(() -> ResponseException.badRequest(ErrorCode.INVALID_PHONE));

		// A retried request (double tap, flaky network) returns the order it already created.
		Order existing = orderRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
		if (existing != null) {
			boolean sameOwner = userId != null ? userId.equals(existing.getUserId())
					: existing.getUserId() == null && existing.getPhone().equals(phone);
			if (!sameOwner) {
				throw ResponseException.conflict(ErrorCode.IDEMPOTENCY_KEY_REUSED);
			}
			return view(existing, null);
		}

		rateLimiter.hit("order:ip:" + ip, 15, Duration.ofHours(1));
		rateLimiter.hit("order:phone:" + phone, 10, Duration.ofHours(1));

		String district = feeCalculator.requireKnown(request.getDistrict());
		PaymentMethod method = request.getPaymentMethod();
		if (!paymentProperties.enabledMethods().contains(method)) {
			throw ResponseException.badRequest(ErrorCode.PAYMENT_METHOD_UNAVAILABLE);
		}
		PaymentGateway gateway = gateways.stream().filter(g -> g.method() == method).findFirst()
				.orElseThrow(() -> ResponseException.badRequest(ErrorCode.PAYMENT_METHOD_UNAVAILABLE));

		List<LineRequest> lines = merge(request.getItems());
		QuoteResponse quote = buildQuote(catalogApi.price(lines), district);
		catalogApi.reserve(lines);

		Instant now = clock.instant();
		Order order = new Order();
		order.setOrderNumber(uniqueOrderNumber());
		order.setUserId(userId);
		order.setCustomerName(request.getFullName().trim());
		order.setPhone(phone);
		order.setEmail(request.getEmail() == null || request.getEmail().isBlank() ? null : request.getEmail().trim().toLowerCase(Locale.ROOT));
		order.setDistrict(district);
		order.setArea(request.getArea().trim());
		order.setAddressLine(request.getAddressLine().trim());
		order.setDeliveryNote(request.getNote() == null || request.getNote().isBlank() ? null : request.getNote().trim());
		order.setStatus(OrderStatus.PLACED);
		order.setPaymentMethod(method);
		order.setSubtotal(quote.subtotal());
		order.setDeliveryFee(quote.deliveryFee());
		order.setTotal(quote.total());
		order.setDepositTotal(quote.depositTotal());
		order.setIdempotencyKey(idempotencyKey);
		order.setPlacedAt(now);
		for (QuoteResponse.Line l : quote.lines()) {
			OrderItem item = new OrderItem();
			item.setOrder(order);
			item.setProductId(l.productId());
			item.setOptionId(l.optionId());
			item.setProductSlug(l.slug());
			item.setProductName(l.name());
			item.setOptionLabel(l.optionLabel());
			item.setUnitPrice(l.unitPrice());
			item.setQuantity(l.quantity());
			item.setLineTotal(l.lineTotal());
			item.setPreorder(l.preorder());
			item.setDepositPerUnit(l.depositPerUnit());
			item.setThumbnailUrl(l.thumbnailUrl());
			order.getItems().add(item);
		}
		orderRepository.save(order);
		historyRepository.save(history(order, null, OrderStatus.PLACED, null, userId == null ? "GUEST" : String.valueOf(userId), now));

		Payment payment = new Payment();
		payment.setOrderId(order.getId());
		payment.setMethod(method);
		payment.setStatus(PaymentStatus.PENDING);
		payment.setAmount(order.getTotal());
		paymentRepository.save(payment);

		events.publishEvent(new OrderPlacedEvent(order.getId(), order.getOrderNumber(), userId,
				lines.stream().map(l -> new OrderPlacedEvent.Line(l.productId(), l.optionId(), l.quantity())).toList(),
				new OrderPlacedEvent.Address(order.getCustomerName(), phone, district, order.getArea(), order.getAddressLine()),
				request.isSaveAddress()));

		log.info("Order {} placed: {} lines, total {} {}, {} for {}", order.getOrderNumber(), order.getItems().size(),
				order.getTotal(), method, userId == null ? "guest" : "user " + userId, LogMasks.maskPhone(phone));
		return view(order, gateway.start(order, payment));
	}

	/** Same product + option twice in one request becomes one line. */
	static List<LineRequest> merge(List<CheckoutLine> items) {
		Map<String, LineRequest> merged = new LinkedHashMap<>();
		for (CheckoutLine i : items) {
			String key = i.getProductId() + ":" + i.getOptionId();
			LineRequest prev = merged.get(key);
			int qty = (prev == null ? 0 : prev.quantity()) + i.getQuantity();
			merged.put(key, new LineRequest(i.getProductId(), i.getOptionId(), Math.min(qty, 10)));
		}
		if (merged.isEmpty()) {
			throw ResponseException.badRequest(ErrorCode.EMPTY_ORDER);
		}
		if (merged.size() > MAX_LINES) {
			throw ResponseException.badRequest(ErrorCode.TOO_MANY_LINES);
		}
		return new ArrayList<>(merged.values());
	}

	QuoteResponse buildQuote(List<PricedLine> priced, String district) {
		List<QuoteResponse.Line> lines = new ArrayList<>();
		BigDecimal subtotal = Money.ZERO;
		BigDecimal deposits = Money.ZERO;
		boolean allInStock = true;
		for (PricedLine p : priced) {
			BigDecimal lineTotal = Money.times(p.unitPrice(), p.quantity());
			subtotal = Money.add(subtotal, lineTotal);
			if (p.preorder()) {
				deposits = Money.add(deposits, Money.times(p.depositPerUnit(), p.quantity()));
			}
			allInStock &= p.enoughStock();
			String label = p.optionValue() == null ? null : p.optionName() + ": " + p.optionValue();
			lines.add(new QuoteResponse.Line(p.productId(), p.optionId(), p.slug(), p.name(), label, p.unitPrice(),
					p.quantity(), lineTotal, p.preorder(), p.preorderRelease(), p.depositPerUnit(), p.stockState(),
					p.enoughStock(), p.thumbnailUrl()));
		}
		BigDecimal fee = feeCalculator.fee(district, subtotal);
		BigDecimal threshold = Money.of(deliveryProperties.freeThreshold());
		BigDecimal toFree = subtotal.compareTo(threshold) >= 0 ? Money.ZERO : Money.of(threshold.subtract(subtotal));
		return new QuoteResponse(lines, subtotal, district, fee, Money.add(subtotal, fee), deposits, threshold, toFree,
				feeCalculator.estimate(district), allInStock);
	}

	private OrderResponse view(Order order, PaymentGateway.PaymentAction action) {
		PaymentStatus status = paymentRepository.findFirstByOrderIdOrderByIdDesc(order.getId())
				.map(Payment::getStatus).orElse(PaymentStatus.PENDING);
		return OrderResponse.from(order, historyRepository.findByOrderIdOrderByIdAsc(order.getId()), status,
				feeCalculator.estimate(order.getDistrict()), action);
	}

	private String uniqueOrderNumber() {
		for (int attempt = 0; attempt < 5; attempt++) {
			String candidate = orderNumberGenerator.next();
			if (!orderRepository.existsByOrderNumber(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("Could not generate a unique order number");
	}

	static OrderStatusHistory history(Order order, OrderStatus from, OrderStatus to, String note, String by, Instant at) {
		OrderStatusHistory h = new OrderStatusHistory();
		h.setOrder(order);
		h.setFromStatus(from);
		h.setToStatus(to);
		h.setNote(note);
		h.setChangedBy(by);
		h.setChangedAt(at);
		return h;
	}
}
