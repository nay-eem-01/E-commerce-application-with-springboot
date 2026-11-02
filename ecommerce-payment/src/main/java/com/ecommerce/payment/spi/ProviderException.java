package com.ecommerce.payment.spi;

/**
 * A vendor or network failure. The message is for logs only (never contains credentials) and
 * is never shown to the shopper; the service turns this into a 502 with an ErrorCode message.
 */
public class ProviderException extends RuntimeException {

	public ProviderException(String message) {
		super(message);
	}

	public ProviderException(String message, Throwable cause) {
		super(message, cause);
	}
}
