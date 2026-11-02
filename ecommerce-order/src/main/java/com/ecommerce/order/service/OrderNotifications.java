package com.ecommerce.order.service;

import com.ecommerce.common.event.OrderNotificationEvent;
import com.ecommerce.order.entity.Order;

/** Builds the snapshot the notification module needs, so it never reads order tables. */
public final class OrderNotifications {
	private OrderNotifications() {}

	public static OrderNotificationEvent event(OrderNotificationEvent.Type type, Order o, String estimate, String note) {
		return new OrderNotificationEvent(type, new OrderNotificationEvent.OrderSummary(
				o.getOrderNumber(), o.getCustomerName(), o.getEmail(), o.getPaymentMethod().name(),
				o.getPaymentMethod().isOnline(),
				o.getItems().stream().map(i -> new OrderNotificationEvent.Line(i.getProductName(), i.getOptionLabel(),
						i.getQuantity(), i.getLineTotal())).toList(),
				o.getSubtotal(), o.getDeliveryFee(), o.getTotal(), o.getDistrict(), estimate, o.getPlacedAt()), note);
	}
}
