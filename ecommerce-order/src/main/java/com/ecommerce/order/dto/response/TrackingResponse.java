package com.ecommerce.order.dto.response;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatusHistory;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Guest tracking by order number + phone. Shows progress and what was ordered, never the
 * address, phone or email: whoever holds an order number should not learn where someone lives.
 */
public record TrackingResponse(
		String orderNumber,
		OrderStatus status,
		Instant placedAt,
		List<Item> items,
		BigDecimal total,
		PaymentMethod paymentMethod,
		String deliveryEstimate,
		List<OrderResponse.TimelineEntry> timeline
) {
	public record Item(String name, String optionLabel, int quantity) {}

	public static TrackingResponse from(Order o, List<OrderStatusHistory> history, String estimate) {
		return new TrackingResponse(o.getOrderNumber(), o.getStatus(), o.getPlacedAt(),
				o.getItems().stream().map(i -> new Item(i.getProductName(), i.getOptionLabel(), i.getQuantity())).toList(),
				o.getTotal(), o.getPaymentMethod(), estimate,
				history.stream().map(OrderResponse.TimelineEntry::from).toList());
	}
}
