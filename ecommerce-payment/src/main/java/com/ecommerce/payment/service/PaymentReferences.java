package com.ecommerce.payment.service;

import java.security.SecureRandom;

/** "PAY-" + 12 Crockford base32 characters (60 random bits). Fits every vendor's id limits. */
public final class PaymentReferences {
	private PaymentReferences() {}

	private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
	private static final SecureRandom RANDOM = new SecureRandom();

	public static String next() {
		StringBuilder sb = new StringBuilder("PAY-");
		for (int i = 0; i < 12; i++) {
			sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
		}
		return sb.toString();
	}
}
