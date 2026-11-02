package com.ecommerce.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Something a shopper should hear about. Published by the order module inside the
 * transaction that made the change; the notification module turns it into an outbox row in
 * that same transaction, so a message exists only if the change committed. Carries a snapshot
 * so the notification module never reads order tables.
 */
public record OrderNotificationEvent(Type type, OrderSummary order, String note) {

	public enum Type {
		PLACED,
		PAYMENT_RECEIVED,
		SHIPPED,
		DELIVERED,
		CANCELLED
	}

	public record OrderSummary(
			String orderNumber,
			String customerName,
			/** Null when the shopper gave no email: nothing is sent to them. */
			String email,
			String paymentMethod,
			boolean onlinePayment,
			List<Line> lines,
			BigDecimal subtotal,
			BigDecimal deliveryFee,
			BigDecimal total,
			String district,
			String deliveryEstimate,
			Instant placedAt
	) {}

	public record Line(String name, String optionLabel, int quantity, BigDecimal lineTotal) {}
}
