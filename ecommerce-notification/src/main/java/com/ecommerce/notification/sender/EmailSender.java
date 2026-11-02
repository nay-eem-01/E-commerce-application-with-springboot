package com.ecommerce.notification.sender;

/**
 * Delivers one email. Implementations: "smtp" and "log", chosen by app.mail.provider. A new
 * transport (SES, SendGrid API, ...) is one class with a new id; nothing else changes.
 * Throw {@link EmailDeliveryException} on failure; the dispatcher retries.
 */
public interface EmailSender {

	String id();

	void send(Email email);

	record Email(String from, String to, String subject, String text, String html) {}
}
