package com.ecommerce.order.service;

import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.order.config.DeliveryProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryFeeCalculatorTest {

	private final DeliveryFeeCalculator calc = new DeliveryFeeCalculator(new DeliveryProperties(
			new BigDecimal("70"), new BigDecimal("130"), new BigDecimal("3000"),
			List.of("Dhaka"), List.of("Dhaka", "Sylhet", "Cox's Bazar"), "1 to 2 days", "2 to 4 days"));

	@Test
	void insideAndOutsideDhaka() {
		assertThat(calc.fee("Dhaka", new BigDecimal("1450"))).isEqualByComparingTo("70.00");
		assertThat(calc.fee("Sylhet", new BigDecimal("1450"))).isEqualByComparingTo("130.00");
	}

	@Test
	void freeAtExactlyTheThresholdNotJustAbove() {
		assertThat(calc.fee("Sylhet", new BigDecimal("2999.99"))).isEqualByComparingTo("130.00");
		assertThat(calc.fee("Sylhet", new BigDecimal("3000.00"))).isEqualByComparingTo("0.00");
	}

	@Test
	void districtMatchIsCaseInsensitiveAndReturnsCanonicalSpelling() {
		assertThat(calc.requireKnown("cox's bazar")).isEqualTo("Cox's Bazar");
		assertThat(calc.isInside("DHAKA")).isTrue();
	}

	@Test
	void unknownDistrictIsRejected() {
		assertThatThrownBy(() -> calc.fee("Atlantis", BigDecimal.TEN)).isInstanceOf(ResponseException.class);
	}
}
