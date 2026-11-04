package com.ecommerce.insights.service;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.time.ShopTime;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Shop days {@code from}..{@code to}, both inclusive. */
public record Period(LocalDate from, LocalDate to) {

	static final int DEFAULT_DAYS = 30;
	static final int MAX_DAYS = 366;

	/** Missing ends default to the 30 days ending today. */
	public static Period of(LocalDate from, LocalDate to, Clock clock) {
		LocalDate end = to != null ? to : LocalDate.now(clock.withZone(ShopTime.ZONE));
		LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS - 1);
		if (start.isAfter(end)) throw ResponseException.badRequest(ErrorCode.INVALID_PERIOD);
		Period p = new Period(start, end);
		if (p.days() > MAX_DAYS) throw ResponseException.badRequest(ErrorCode.PERIOD_TOO_LONG);
		return p;
	}

	public int days() {
		return (int) ChronoUnit.DAYS.between(from, to) + 1;
	}

	public Period previous() {
		return new Period(from.minusDays(days()), from.minusDays(1));
	}

	public Instant start() {
		return from.atStartOfDay(ShopTime.ZONE).toInstant();
	}

	public Instant endExclusive() {
		return to.plusDays(1).atStartOfDay(ShopTime.ZONE).toInstant();
	}
}
