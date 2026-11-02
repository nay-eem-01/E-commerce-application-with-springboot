package com.ecommerce.payment.spi;

import com.ecommerce.payment.api.PaymentMethod;

import java.util.Map;
import java.util.Set;

/**
 * One vendor integration (bKash, SSLCommerz, ...). Adding a vendor is one new implementation
 * annotated @Component; choosing it for a method is the property app.payment.routes.<METHOD>.
 *
 * Contract every implementation keeps:
 * - Never trust browser parameters: {@link #verify} and {@link #query} ask the vendor's server.
 * - Report the amount and currency the vendor says were paid; the service compares them.
 * - Never log credentials or tokens.
 * - Throw {@link ProviderException} for vendor/network failures, nothing vendor-specific.
 */
public interface PaymentProvider {

	/** Stable id used in configuration and callback URLs, e.g. "bkash". Lowercase. */
	String id();

	Set<PaymentMethod> supportedMethods();

	/** True when every credential this provider needs is present. */
	boolean isConfigured();

	/** Opens a payment; returns the vendor's page URL and the vendor's payment id if it has one. */
	Initiation initiate(InitiationRequest request);

	/** Which of our payments a callback or IPN is about, read from its parameters. */
	CallbackKey keyOf(Map<String, String> callbackParams);

	/** Confirms a callback/IPN with the vendor's server. */
	Verification verify(PaymentRef payment, Map<String, String> callbackParams);

	/** Asks the vendor for the current state of a payment (used before expiring one). */
	Verification query(PaymentRef payment);

	/** Refunds a paid payment in full. */
	Refund refund(PaymentRef payment, String reason);

	record InitiationRequest(
			String reference,
			String orderNumber,
			PaymentMethod method,
			java.math.BigDecimal amount,
			String customerName,
			String customerPhone,
			String customerEmail,
			String district,
			String addressLine,
			int itemCount,
			String callbackUrl,
			String ipnUrl
	) {}

	record Initiation(String redirectUrl, String providerPaymentId) {}

	/** Either our reference or the vendor's payment id identifies the payment. */
	record CallbackKey(String reference, String providerPaymentId) {}

	/** What the service knows about one of our payments, handed to the provider. */
	record PaymentRef(String reference, String providerPaymentId, String providerTransactionId, java.math.BigDecimal amount, String currency) {}

	enum Outcome { PAID, PENDING, FAILED, CANCELLED }

	record Verification(Outcome outcome, String providerTransactionId, java.math.BigDecimal amount, String currency, String detail) {

		public static Verification pending(String detail) {
			return new Verification(Outcome.PENDING, null, null, null, detail);
		}
	}

	record Refund(boolean succeeded, String providerRefundId, String detail) {}
}
