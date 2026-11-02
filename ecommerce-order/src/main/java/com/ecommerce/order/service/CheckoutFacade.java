package com.ecommerce.order.service;

import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.order.dto.request.PlaceOrderRequest;
import com.ecommerce.order.dto.response.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.payment.api.OnlinePaymentRequest;
import com.ecommerce.payment.api.PaymentAction;
import com.ecommerce.payment.api.PaymentApi;
import com.ecommerce.payment.api.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Not transactional on purpose. Places the order (one transaction, committed), then for an
 * online method opens the payment with the provider, outside any transaction. If the provider
 * is down the order still exists with paymentStatus FAILED and paymentAction null; the
 * storefront offers "Try paying again" (POST /api/orders/pay).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutFacade {

	private final CheckoutService checkoutService;
	private final OrderRepository orderRepository;
	private final PaymentApi paymentApi;

	public OrderResponse placeAndPay(PlaceOrderRequest request, String idempotencyKey, Long userId, String ip) {
		OrderResponse placed = checkoutService.place(request, idempotencyKey, userId, ip);
		if (!placed.paymentMethod().isOnline() || placed.paymentStatus() == PaymentStatus.PAID) {
			return placed;
		}
		Order order = orderRepository.findByOrderNumber(placed.orderNumber()).orElseThrow();
		if (paymentApi.hasAttempt(order.getId())) {
			// A retried checkout request: the first one already opened a payment.
			return placed;
		}
		try {
			PaymentAction action = paymentApi.startOnline(paymentRequest(order));
			return placed.withPayment(paymentApi.statusOf(order.getId()), action);
		} catch (ResponseException providerDown) {
			log.warn("Order {} placed but its payment could not be started", order.getOrderNumber());
			return placed.withPayment(paymentApi.statusOf(order.getId()), null);
		}
	}

	static OnlinePaymentRequest paymentRequest(Order o) {
		return new OnlinePaymentRequest(o.getId(), o.getOrderNumber(), o.getPaymentMethod(), o.getTotal(),
				o.getCustomerName(), o.getPhone(), o.getEmail() == null ? null : o.getEmail().toLowerCase(Locale.ROOT),
				o.getDistrict(), o.getAddressLine(), o.getItems().stream().mapToInt(i -> i.getQuantity()).sum());
	}
}
