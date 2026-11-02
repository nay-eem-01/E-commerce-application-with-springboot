package com.ecommerce.order.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * "SS-" + 8 Crockford base32 characters (40 random bits). Random, not sequential, so an order
 * number does not reveal sales volume and cannot be guessed; guest tracking also needs the
 * phone number and is rate limited. Uniqueness is enforced by a database constraint.
 */
@Component
public class OrderNumberGenerator {

	private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
	private static final SecureRandom RANDOM = new SecureRandom();

	public String next() {
		StringBuilder sb = new StringBuilder("SS-");
		for (int i = 0; i < 8; i++) {
			sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
		}
		return sb.toString();
	}
}
