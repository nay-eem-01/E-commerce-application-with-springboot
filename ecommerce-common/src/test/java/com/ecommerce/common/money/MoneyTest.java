package com.ecommerce.common.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

	@Test
	void roundsHalfUpToTwoDecimals() {
		assertThat(Money.of(new BigDecimal("10.005"))).isEqualByComparingTo("10.01");
		assertThat(Money.of(new BigDecimal("10.004"))).isEqualByComparingTo("10.00");
	}

	@Test
	void multipliesWithoutFloatingPointDrift() {
		// 0.1 * 3 is 0.30000000000000004 in double; must be exactly 0.30 here.
		assertThat(Money.times(new BigDecimal("0.10"), 3)).isEqualByComparingTo("0.30");
		assertThat(Money.times(new BigDecimal("6800.00"), 2).scale()).isEqualTo(2);
	}

	@Test
	void nullIsZero() {
		assertThat(Money.of((BigDecimal) null)).isEqualByComparingTo("0.00");
	}
}
