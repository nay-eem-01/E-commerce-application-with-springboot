package com.ecommerce.insights.service;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodTest {

	// 02:00 on 5 Nov in Dhaka
	private final Clock clock = Clock.fixed(Instant.parse("2026-11-04T20:00:00Z"), ZoneOffset.UTC);

	@Test
	void defaultsToThirtyDaysEndingTodayInDhaka() {
		Period p = Period.of(null, null, clock);
		assertThat(p.from()).isEqualTo(LocalDate.of(2026, 10, 7));
		assertThat(p.to()).isEqualTo(LocalDate.of(2026, 11, 5));
		assertThat(p.days()).isEqualTo(30);
	}

	@Test
	void fromAfterToIsRejected() {
		assertThatThrownBy(() -> Period.of(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1), clock))
				.isInstanceOf(ResponseException.class).hasMessage(ErrorCode.INVALID_PERIOD);
	}

	@Test
	void longerThan366DaysIsRejected() {
		LocalDate from = LocalDate.of(2025, 1, 1);
		assertThat(Period.of(from, from.plusDays(365), clock).days()).isEqualTo(366);
		assertThatThrownBy(() -> Period.of(from, from.plusDays(366), clock))
				.isInstanceOf(ResponseException.class).hasMessage(ErrorCode.PERIOD_TOO_LONG);
	}

	@Test
	void previousIsTheSameLengthJustBefore() {
		Period p = Period.of(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), clock).previous();
		assertThat(p.from()).isEqualTo(LocalDate.of(2026, 10, 2));
		assertThat(p.to()).isEqualTo(LocalDate.of(2026, 10, 31));
	}

	@Test
	void boundsAreDhakaMidnights() {
		Period p = Period.of(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1), clock);
		assertThat(p.start()).isEqualTo(Instant.parse("2026-10-31T18:00:00Z"));
		assertThat(p.endExclusive()).isEqualTo(Instant.parse("2026-11-01T18:00:00Z"));
	}
}
