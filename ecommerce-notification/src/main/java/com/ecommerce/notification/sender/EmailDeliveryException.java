package com.ecommerce.notification.sender;

/** A transport failure. Message is for logs only and never contains credentials. */
public class EmailDeliveryException extends RuntimeException {

	public EmailDeliveryException(String message, Throwable cause) {
		super(message, cause);
	}
}
