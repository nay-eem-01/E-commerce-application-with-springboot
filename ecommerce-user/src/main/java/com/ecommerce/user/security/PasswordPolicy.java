package com.ecommerce.user.security;

import java.util.regex.Pattern;

/** One password policy for every path that sets a password. */
public final class PasswordPolicy {
	private PasswordPolicy() {}

	private static final Pattern LETTER_DIGIT_SPECIAL = Pattern.compile("^(?=.*\\d)(?=.*[A-Za-z])(?=.*[\\W_]).+$");

	/** Null when the password is acceptable, otherwise the message to show. */
	public static String problemWith(String password) {
		if (password == null || password.isEmpty()) {
			return "Enter a password.";
		}
		if (password.contains(" ")) {
			return "A password cannot contain spaces.";
		}
		if (password.length() < 8 || password.length() > 64) {
			return "Use 8 to 64 characters for your password.";
		}
		if (!LETTER_DIGIT_SPECIAL.matcher(password).matches()) {
			return "Use at least one letter, one number and one symbol in your password.";
		}
		return null;
	}
}
