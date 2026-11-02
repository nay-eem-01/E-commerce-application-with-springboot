package com.ecommerce.common.logging;

/**
 * Masking for the few personal values that may appear in a log line at all. Passwords,
 * OTP codes and tokens are never logged in any form.
 */
public final class LogMasks {
	private LogMasks() {}

	public static final String REDACTED = "***";

	/** +8801712345678 -> +8801XXXXX678 */
	public static String maskPhone(String phone) {
		if (phone == null || phone.length() < 8) {
			return REDACTED;
		}
		int keepEnd = 3;
		int keepStart = phone.length() - keepEnd - 5;
		if (keepStart < 1) {
			return REDACTED;
		}
		return phone.substring(0, keepStart) + "XXXXX" + phone.substring(phone.length() - keepEnd);
	}

	/** shopper@example.com -> s******@example.com */
	public static String maskEmail(String email) {
		if (email == null) {
			return REDACTED;
		}
		int at = email.indexOf('@');
		if (at < 1) {
			return REDACTED;
		}
		return email.charAt(0) + "*".repeat(Math.max(1, at - 1)) + email.substring(at);
	}
}
