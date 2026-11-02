package com.ecommerce.order.dto.response;

import com.ecommerce.common.money.Money;
import com.ecommerce.common.phone.PhoneNumbers;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatusHistory;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.enums.PaymentMethod;
import com.ecommerce.order.enums.PaymentStatus;
import com.ecommerce.order.payment.PaymentGateway;
import com.ecommerce.order.service.OrderStateMachine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** The full order, for its owner (or staff). */
public record OrderResponse(
		String orderNumber,
		OrderStatus status,
		Instant placedAt,
		Instant deliveredAt,
		String fullName,
		String phone,
		String email,
		String district,
		String area,
		String addressLine,
		String note,
		PaymentMethod paymentMethod,
		PaymentStatus paymentStatus,
		List<ItemResponse> items,
		BigDecimal subtotal,
		BigDecimal deliveryFee,
		BigDecimal total,
		BigDecimal depositTotal,
		/** What the rider collects: the total for COD while unpaid, otherwise zero. */
		BigDecimal amountDueOnDelivery,
		String deliveryEstimate,
		List<TimelineEntry> timeline,
		boolean canCancel,
		/** Set only right after placing an order with an online method. */
		PaymentGateway.PaymentAction paymentAction
) {
	public record ItemResponse(Long productId, Long optionId, String slug, String name, String optionLabel,
							   BigDecimal unitPrice, int quantity, BigDecimal lineTotal, boolean preorder,
							   BigDecimal depositPerUnit, String thumbnailUrl) {
		public static ItemResponse from(OrderItem i) {
			return new ItemResponse(i.getProductId(), i.getOptionId(), i.getProductSlug(), i.getProductName(),
					i.getOptionLabel(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal(), i.isPreorder(),
					i.getDepositPerUnit(), i.getThumbnailUrl());
		}
	}

	public record TimelineEntry(OrderStatus status, Instant at, String note) {
		public static TimelineEntry from(OrderStatusHistory h) {
			return new TimelineEntry(h.getToStatus(), h.getChangedAt(), h.getNote());
		}
	}

	public static OrderResponse from(Order o, List<OrderStatusHistory> history, PaymentStatus paymentStatus,
									 String estimate, PaymentGateway.PaymentAction action) {
		BigDecimal due = o.getPaymentMethod() == PaymentMethod.COD && paymentStatus == PaymentStatus.PENDING
				&& o.getStatus() != OrderStatus.CANCELLED ? o.getTotal() : Money.ZERO;
		return new OrderResponse(
				o.getOrderNumber(), o.getStatus(), o.getPlacedAt(), o.getDeliveredAt(),
				o.getCustomerName(), PhoneNumbers.toLocal(o.getPhone()), o.getEmail(),
				o.getDistrict(), o.getArea(), o.getAddressLine(), o.getDeliveryNote(),
				o.getPaymentMethod(), paymentStatus,
				o.getItems().stream().map(ItemResponse::from).toList(),
				o.getSubtotal(), o.getDeliveryFee(), o.getTotal(), o.getDepositTotal(), due, estimate,
				history.stream().map(TimelineEntry::from).toList(),
				OrderStateMachine.customerMayCancel(o.getStatus()),
				action
		);
	}
}
