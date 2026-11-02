package com.ecommerce.user.sms;

/**
 * Sends one SMS. The provider is chosen by SMS_PROVIDER. A real Bangladeshi provider
 * (sender-ID approval takes days) plugs in as one class; nothing else changes.
 */
public interface SmsSender {

	void send(String canonicalPhone, String message);
}
