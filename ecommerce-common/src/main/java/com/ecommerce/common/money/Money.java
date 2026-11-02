package com.ecommerce.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money is BigDecimal in BDT with two decimals, stored as NUMERIC(12,2). Never double.
 * All rounding goes through here so one RoundingMode applies everywhere.
 */
public final class Money {
	private Money() {}

	public static final int SCALE = 2;
	public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
	public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING);

	public static BigDecimal of(BigDecimal value) {
		return value == null ? ZERO : value.setScale(SCALE, ROUNDING);
	}

	public static BigDecimal of(long value) {
		return BigDecimal.valueOf(value).setScale(SCALE, ROUNDING);
	}

	public static BigDecimal times(BigDecimal unit, int quantity) {
		return of(unit.multiply(BigDecimal.valueOf(quantity)));
	}

	public static BigDecimal add(BigDecimal a, BigDecimal b) {
		return of(of(a).add(of(b)));
	}
}
