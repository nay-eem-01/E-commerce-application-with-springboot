package com.ecommerce.order.enums;

public enum PaymentStatus {
	/** COD not collected yet, or an online payment not confirmed yet. */
	PENDING,
	PAID,
	FAILED,
	VOIDED,
	REFUNDED
}
