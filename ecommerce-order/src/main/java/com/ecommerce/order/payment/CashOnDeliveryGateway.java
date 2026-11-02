package com.ecommerce.order.payment;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.Payment;
import com.ecommerce.order.enums.PaymentMethod;
import org.springframework.stereotype.Component;

/** Nothing is charged at checkout; the payment is marked PAID when the order is delivered. */
@Component
public class CashOnDeliveryGateway implements PaymentGateway {

	@Override
	public PaymentMethod method() {
		return PaymentMethod.COD;
	}

	@Override
	public PaymentAction start(Order order, Payment payment) {
		return PaymentAction.none();
	}
}
