package com.ecommerce.payment.api;

public enum PaymentStatus {
	/** COD not collected yet, or an online payment the provider has not confirmed. */
	PENDING,
	PAID,
	FAILED,
	CANCELLED,
	/** An online payment not completed within app.payment.pending-ttl. */
	EXPIRED,
	REFUNDED;

	public boolean isFinal() {
		return this == PAID || this == REFUNDED || this == CANCELLED || this == EXPIRED;
	}
}
