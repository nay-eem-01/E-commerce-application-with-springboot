package com.ecommerce.order.service;

import com.ecommerce.catalog.api.CatalogApi;
import com.ecommerce.catalog.api.LineRequest;
import com.ecommerce.common.constant.AppConstants;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.model.PageResponse;
import com.ecommerce.common.phone.PhoneNumbers;
import com.ecommerce.common.ratelimit.RateLimiter;
import com.ecommerce.common.web.Pages;
import com.ecommerce.order.dto.response.OrderResponse;
import com.ecommerce.order.dto.response.OrderSummaryResponse;
import com.ecommerce.order.dto.response.TrackingResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.enums.OrderAction;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.payment.api.PaymentAction;
import com.ecommerce.payment.api.PaymentApi;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

/**
 * Reading and changing existing orders. Shoppers reach only their own orders (scoped
 * queries, so another shopper's order is a 404). Staff changes go through the state machine.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

	static final Set<String> SORTS = Set.of(AppConstants.SORT_BY_VALUE, "placedAt", "total");

	private final OrderRepository orderRepository;
	private final OrderStatusHistoryRepository historyRepository;
	private final PaymentApi paymentApi;
	private final DeliveryFeeCalculator feeCalculator;
	private final CatalogApi catalogApi;
	private final RateLimiter rateLimiter;
	private final Clock clock;

	@Transactional(readOnly = true)
	public PageResponse<OrderSummaryResponse> myOrders(Long userId, int pageNo, int pageSize) {
		return PageResponse.of(orderRepository.findByUserId(userId,
				Pages.of(pageNo, pageSize, "placedAt", AscOrDescType.desc, SORTS)), OrderSummaryResponse::from);
	}

	@Transactional(readOnly = true)
	public OrderResponse myOrder(Long userId, String orderNumber) {
		return view(mine(userId, orderNumber));
	}

	@Transactional
	public OrderResponse cancelMine(Long userId, String orderNumber) {
		Order order = mine(userId, orderNumber);
		if (!OrderStateMachine.customerMayCancel(order.getStatus())) {
			throw ResponseException.conflict(ErrorCode.ORDER_NOT_CANCELLABLE);
		}
		apply(order, OrderAction.CANCEL, "Cancelled by the customer", String.valueOf(userId));
		return view(order);
	}

	/** Guest tracking: order number + the phone it was placed with. Wrong pairs are a plain 404. */
	@Transactional(readOnly = true)
	public TrackingResponse track(String orderNumber, String rawPhone, String ip) {
		rateLimiter.hit("track:ip:" + ip, 20, Duration.ofMinutes(15));
		String phone = PhoneNumbers.normalize(rawPhone).orElseThrow(() -> ResponseException.notFound(ErrorCode.ORDER_NOT_FOUND));
		Order order = orderRepository.findByOrderNumberAndPhone(orderNumber.trim().toUpperCase(Locale.ROOT), phone)
				.orElseThrow(() -> ResponseException.notFound(ErrorCode.ORDER_NOT_FOUND));
		return TrackingResponse.from(order, historyRepository.findByOrderIdOrderByIdAsc(order.getId()),
				feeCalculator.estimate(order.getDistrict()));
	}

	@Transactional(readOnly = true)
	public PageResponse<OrderSummaryResponse> search(OrderStatus status, String q, int pageNo, int pageSize, String sortBy, AscOrDescType dir) {
		// Never bind null into a string function: PostgreSQL types it as bytea. '' means "no filter".
		String query = q == null ? "" : q.trim().toUpperCase(Locale.ROOT);
		return PageResponse.of(orderRepository.search(status, query, Pages.of(pageNo, pageSize, sortBy, dir, SORTS)),
				OrderSummaryResponse::from);
	}

	@Transactional(readOnly = true)
	public OrderResponse adminGet(String orderNumber) {
		return view(byNumber(orderNumber));
	}

	@Transactional
	public OrderResponse adminApply(String orderNumber, OrderAction action, String note, Long staffId) {
		Order order = byNumber(orderNumber);
		apply(order, action, note, String.valueOf(staffId));
		return view(order);
	}

	/**
	 * Retry an online payment: guests and members alike, with the order number and the phone it
	 * was placed with. Not transactional: the provider call must not hold a transaction open.
	 */
	public PaymentAction retryPayment(String orderNumber, String rawPhone, String ip) {
		rateLimiter.hit("pay-retry:ip:" + ip, 10, Duration.ofMinutes(15));
		String phone = PhoneNumbers.normalize(rawPhone).orElseThrow(() -> ResponseException.notFound(ErrorCode.ORDER_NOT_FOUND));
		Order order = orderRepository.findByOrderNumberAndPhone(orderNumber.trim().toUpperCase(Locale.ROOT), phone)
				.orElseThrow(() -> ResponseException.notFound(ErrorCode.ORDER_NOT_FOUND));
		if (order.getStatus() != OrderStatus.PLACED || !order.getPaymentMethod().isOnline() || paymentApi.isPaid(order.getId())) {
			throw ResponseException.conflict(ErrorCode.PAYMENT_NOT_RETRYABLE);
		}
		return paymentApi.startOnline(CheckoutFacade.paymentRequest(order));
	}

	/** An online payment expired: cancel the order and put the stock back (runs in the payment's transaction). */
	@Transactional
	public void cancelUnpaid(Long orderId, String paymentReference) {
		orderRepository.findById(orderId)
				.filter(o -> o.getStatus() == OrderStatus.PLACED && o.getPaymentMethod().isOnline())
				.filter(o -> !paymentApi.isPaid(o.getId()))
				.ifPresent(o -> apply(o, OrderAction.CANCEL, "Payment " + paymentReference + " was not completed in time", "SYSTEM"));
	}

	/**
	 * Moves the order, writes the history row and applies side effects in one transaction:
	 * cancel puts stock back and settles the payment (cancels it, or refunds a paid online
	 * payment); deliver marks COD as collected. Online orders are packed only once paid.
	 * A returned item's stock is not restored automatically - staff restock after inspection.
	 */
	private void apply(Order order, OrderAction action, String note, String by) {
		OrderStatus from = order.getStatus();
		OrderStatus to = OrderStateMachine.next(from, action);
		if (action == OrderAction.PACK && order.getPaymentMethod().isOnline() && !paymentApi.isPaid(order.getId())) {
			throw ResponseException.conflict(ErrorCode.ORDER_NOT_PAID);
		}
		Instant now = clock.instant();
		order.setStatus(to);
		historyRepository.save(CheckoutService.history(order, from, to, blankToNull(note), by, now));

		switch (to) {
			case CANCELLED -> {
				catalogApi.release(order.getItems().stream()
						.map(i -> new LineRequest(i.getProductId(), i.getOptionId(), i.getQuantity())).toList());
				paymentApi.settleCancellation(order.getId());
			}
			case DELIVERED -> {
				order.setDeliveredAt(now);
				paymentApi.markCashCollected(order.getId());
			}
			default -> { }
		}
		log.info("Order {} {} -> {} by {}", order.getOrderNumber(), from, to, by);
	}

	private Order mine(Long userId, String orderNumber) {
		return orderRepository.findByOrderNumberAndUserId(orderNumber.toUpperCase(Locale.ROOT), userId)
				.orElseThrow(() -> ResponseException.notFound(ErrorCode.ORDER_NOT_FOUND));
	}

	private Order byNumber(String orderNumber) {
		return orderRepository.findByOrderNumber(orderNumber.toUpperCase(Locale.ROOT))
				.orElseThrow(() -> ResponseException.notFound(ErrorCode.ORDER_NOT_FOUND));
	}

	private OrderResponse view(Order order) {
		return OrderResponse.from(order, historyRepository.findByOrderIdOrderByIdAsc(order.getId()),
				paymentApi.statusOf(order.getId()), feeCalculator.estimate(order.getDistrict()), null);
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}
}
