package com.ecommerce.common.ratelimit;

import java.time.Duration;

/**
 * Fixed-window attempt counter (sign-in, OTP, guest order lookup, order spam). Keys are
 * arbitrary strings like "otp:phone:+8801..." so a limit never depends on whether an
 * account exists. The in-memory implementation is correct for one instance; a second
 * instance needs a shared store (Redis) behind this same interface - one new class.
 */
public interface RateLimiter {

	boolean isBlocked(String key);

	void record(String key, int max, Duration window);

	void reset(String key);

	/** Records the attempt and throws 429 if the key is now over its allowance. */
	default void hit(String key, int max, Duration window) {
		if (isBlocked(key)) {
			throw com.ecommerce.common.exception.ResponseException.tooManyRequests();
		}
		record(key, max, window);
	}
}
