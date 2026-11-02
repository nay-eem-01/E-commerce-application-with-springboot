package com.ecommerce.common.ratelimit;

import com.ecommerce.common.exception.ResponseException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryRateLimiterTest {

	private final AtomicLong now = new AtomicLong(1_000_000);
	private final InMemoryRateLimiter limiter = new InMemoryRateLimiter(now::get);

	@Test
	void blocksAfterMaxAttemptsUntilTheWindowEnds() {
		for (int i = 0; i < 3; i++) {
			limiter.hit("k", 3, Duration.ofMinutes(1));
		}
		assertThat(limiter.isBlocked("k")).isTrue();
		assertThatThrownBy(() -> limiter.hit("k", 3, Duration.ofMinutes(1))).isInstanceOf(ResponseException.class);

		now.addAndGet(Duration.ofMinutes(1).toMillis());
		assertThat(limiter.isBlocked("k")).isFalse();
	}

	@Test
	void keysAreIndependentAndResettable() {
		limiter.record("a", 1, Duration.ofMinutes(1));
		assertThat(limiter.isBlocked("a")).isTrue();
		assertThat(limiter.isBlocked("b")).isFalse();
		limiter.reset("a");
		assertThat(limiter.isBlocked("a")).isFalse();
	}
}
