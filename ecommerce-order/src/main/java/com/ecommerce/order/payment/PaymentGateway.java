package com.ecommerce.order.payment;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.Payment;
import com.ecommerce.order.enums.PaymentMethod;

/**
 * One implementation per payment method. A bKash, Nagad or card gateway is a new class
 * plus enabling the method in app.payment.enabled-methods; checkout does not change.
 * Online gateways must confirm payment server-to-server (webhook or verify call), never
 * from a value the browser sends back.
 */
public interface PaymentGateway {

	PaymentMethod method();

	/** Called after the order is saved. Returns what the browser should do next. */
	PaymentAction start(Order order, Payment payment);

	/** NONE: nothing to do (COD). REDIRECT: send the shopper to url to pay. */
	record PaymentAction(String type, String url) {
		public static PaymentAction none() {
			return new PaymentAction("NONE", null);
		}
	}
}
