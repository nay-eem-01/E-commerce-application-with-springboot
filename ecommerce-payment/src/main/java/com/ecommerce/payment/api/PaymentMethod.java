package com.ecommerce.payment.api;

/**
 * What the shopper chooses at checkout. Which vendor processes a method is configuration
 * ({@code app.payment.routes.<METHOD>=<provider id>}), not code.
 */
public enum PaymentMethod {
	/** Cash to the rider. Handled in-house, no provider. */
	COD,
	BKASH,
	NAGAD,
	/** Debit/credit cards and internet banking. */
	CARD;

	public boolean isOnline() {
		return this != COD;
	}
}
