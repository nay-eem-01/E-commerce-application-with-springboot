package com.ecommerce.common.phone;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Bangladeshi mobile numbers. Accepts 01712345678, +8801712345678 or 8801712345678 (spaces
 * and dashes ignored) and stores one canonical form, +8801712345678, so the same person
 * never gets two accounts.
 */
public final class PhoneNumbers {
	private PhoneNumbers() {}

	private static final Pattern LOCAL = Pattern.compile("^01[3-9]\\d{8}$");

	public static Optional<String> normalize(String raw) {
		if (raw == null) {
			return Optional.empty();
		}
		String digits = raw.replaceAll("[\\s-]", "");
		if (digits.startsWith("+")) {
			digits = digits.substring(1);
		}
		if (digits.startsWith("88")) {
			digits = digits.substring(2);
		}
		return LOCAL.matcher(digits).matches() ? Optional.of("+88" + digits) : Optional.empty();
	}

	/** +8801712345678 -> 01712345678, the form shoppers recognise. */
	public static String toLocal(String canonical) {
		return canonical != null && canonical.startsWith("+88") ? canonical.substring(3) : canonical;
	}
}
