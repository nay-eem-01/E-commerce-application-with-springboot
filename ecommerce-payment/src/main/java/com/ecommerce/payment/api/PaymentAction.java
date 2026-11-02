package com.ecommerce.payment.api;

/** What the browser does next: NONE (cash on delivery) or REDIRECT to the provider's page. */
public record PaymentAction(String type, String url) {

	public static PaymentAction none() {
		return new PaymentAction("NONE", null);
	}

	public static PaymentAction redirect(String url) {
		return new PaymentAction("REDIRECT", url);
	}
}
