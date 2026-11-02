package com.ecommerce.order.listener;

import com.ecommerce.common.event.PaymentExpiredEvent;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** An online payment was not completed in time: cancel the order and restock, in the payment's transaction. */
@Component
@RequiredArgsConstructor
public class PaymentExpiredOrderListener {

	private final OrderService orderService;

	@EventListener
	public void on(PaymentExpiredEvent event) {
		orderService.cancelUnpaid(event.orderId(), event.paymentReference());
	}
}
