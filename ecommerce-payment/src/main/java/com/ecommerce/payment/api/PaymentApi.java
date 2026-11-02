package com.ecommerce.payment.api;

import java.math.BigDecimal;
import java.util.List;

/**
 * The payment module's public surface. The order module calls this and never touches a
 * provider, the payments table or a vendor SDK.
 */
public interface PaymentApi {

	/** Methods enabled by configuration and backed by a configured provider, for checkout. */
	List<PaymentMethod> availableMethods();

	boolean isAvailable(PaymentMethod method);

	/** Records a cash-on-delivery payment, PENDING until the rider collects it. */
	void createCashOnDelivery(Long orderId, String orderNumber, BigDecimal amount);

	/**
	 * Opens a payment with the method's provider and returns where to send the browser.
	 * Runs its own short transactions; the provider HTTP call happens outside any transaction.
	 * 502 if the provider is down - the order stays PLACED and the shopper can retry.
	 */
	PaymentAction startOnline(OnlinePaymentRequest request);

	/** Status of the order's latest payment attempt (PENDING when there is none). */
	PaymentStatus statusOf(Long orderId);

	boolean isPaid(Long orderId);

	/** True once any payment attempt exists for the order. */
	boolean hasAttempt(Long orderId);

	/** Cash handed to the rider: marks a pending COD payment PAID. No-op otherwise. */
	void markCashCollected(Long orderId);

	/**
	 * Order cancelled: a pending payment is cancelled; a paid online payment is refunded
	 * through its provider (502 if the provider refuses, so the cancel rolls back).
	 */
	void settleCancellation(Long orderId);
}
