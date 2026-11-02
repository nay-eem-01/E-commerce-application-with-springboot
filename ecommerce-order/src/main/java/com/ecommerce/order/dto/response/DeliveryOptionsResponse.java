package com.ecommerce.order.dto.response;

import com.ecommerce.order.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

/** Everything the cart and checkout pages show about delivery and payment, from config. */
public record DeliveryOptionsResponse(
		List<String> districts,
		List<String> insideDistricts,
		BigDecimal insideFee,
		BigDecimal outsideFee,
		BigDecimal freeDeliveryThreshold,
		String insideEstimate,
		String outsideEstimate,
		List<PaymentMethod> paymentMethods
) {}
