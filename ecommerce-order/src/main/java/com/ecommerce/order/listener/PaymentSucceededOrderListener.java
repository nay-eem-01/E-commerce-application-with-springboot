package com.ecommerce.order.listener;

import com.ecommerce.common.event.PaymentSucceededEvent;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** A payment was confirmed: the order module decides whether the shopper hears about it. */
@Component
@RequiredArgsConstructor
public class PaymentSucceededOrderListener {

	private final OrderService orderService;

	@EventListener
	public void on(PaymentSucceededEvent event) {
		orderService.announcePayment(event.orderId());
	}
}
