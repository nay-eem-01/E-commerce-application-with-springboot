package com.ecommerce.common.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Ported from AgroTrends' AttemptLimiter. Single-instance only; see {@link RateLimiter}. */
@Component
public class InMemoryRateLimiter implements RateLimiter {

	private static final int EVICTION_THRESHOLD = 10_000;

	private static final class Entry {
		int count;
		long windowStart;
		long windowMillis;
		int max;
	}

	private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
	private final LongSupplier clock;

	public InMemoryRateLimiter() {
		this(System::currentTimeMillis);
	}

	/** Test seam for the clock - the reason this constructor is hand-written. */
	InMemoryRateLimiter(LongSupplier clock) {
		this.clock = clock;
	}

	@Override
	public boolean isBlocked(String key) {
		Entry entry = entries.get(key);
		if (entry == null) {
			return false;
		}
		synchronized (entry) {
			return clock.getAsLong() - entry.windowStart < entry.windowMillis && entry.count >= entry.max;
		}
	}

	@Override
	public void record(String key, int max, Duration window) {
		long now = clock.getAsLong();
		if (entries.size() > EVICTION_THRESHOLD) {
			evictExpired(now);
		}
		Entry entry = entries.computeIfAbsent(key, k -> new Entry());
		synchronized (entry) {
			if (entry.count == 0 || now - entry.windowStart >= entry.windowMillis) {
				entry.count = 0;
				entry.windowStart = now;
			}
			entry.count++;
			entry.max = max;
			entry.windowMillis = window.toMillis();
		}
	}

	@Override
	public void reset(String key) {
		entries.remove(key);
	}

	private void evictExpired(long now) {
		entries.entrySet().removeIf(e -> {
			synchronized (e.getValue()) {
				return now - e.getValue().windowStart >= e.getValue().windowMillis;
			}
		});
	}
}
