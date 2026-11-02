package com.ecommerce.order.dto.response;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** A row in "My orders". Items carry ids so "Buy again" can refill the cart. */
public record OrderSummaryResponse(
		String orderNumber,
		OrderStatus status,
		Instant placedAt,
		BigDecimal total,
		PaymentMethod paymentMethod,
		int itemCount,
		List<OrderResponse.ItemResponse> items
) {
	public static OrderSummaryResponse from(Order o) {
		return new OrderSummaryResponse(o.getOrderNumber(), o.getStatus(), o.getPlacedAt(), o.getTotal(),
				o.getPaymentMethod(), o.getItems().stream().mapToInt(i -> i.getQuantity()).sum(),
				o.getItems().stream().map(OrderResponse.ItemResponse::from).toList());
	}
}
