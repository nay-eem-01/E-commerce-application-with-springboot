package com.ecommerce.payment.api;

import java.math.BigDecimal;

/** Everything a provider may need to open a payment page. Amount always comes from the order. */
public record OnlinePaymentRequest(
		Long orderId,
		String orderNumber,
		PaymentMethod method,
		BigDecimal amount,
		String customerName,
		String customerPhone,
		String customerEmail,
		String district,
		String addressLine,
		int itemCount
) {}
