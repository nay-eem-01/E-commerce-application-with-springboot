package com.ecommerce.order.api;

/** Public surface of the order module for reviews. */
public interface PurchaseVerifier {

	/** True when the user has a delivered order containing the product. */
	boolean hasReceived(Long userId, Long productId);
}
