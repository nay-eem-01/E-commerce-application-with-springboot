package com.ecommerce.user.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Random opaque tokens and their SHA-256, for refresh tokens. */
public final class TokenHasher {
	private TokenHasher() {}

	private static final SecureRandom RANDOM = new SecureRandom();

	/** 256 bits, URL-safe. */
	public static String newToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 unavailable", e);
		}
	}

	/** Six digits, uniform, from a CSPRNG. */
	public static String newOtpCode() {
		return String.format("%06d", RANDOM.nextInt(1_000_000));
	}
}
