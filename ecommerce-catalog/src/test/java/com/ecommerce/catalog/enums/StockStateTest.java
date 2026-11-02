package com.ecommerce.catalog.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class StockStateTest {

	@ParameterizedTest
	@CsvSource({"false,0,SOLD_OUT", "false,1,LOW_STOCK", "false,5,LOW_STOCK", "false,6,IN_STOCK", "true,10,PREORDER", "true,0,SOLD_OUT"})
	void stockLine(boolean preorder, int stock, StockState expected) {
		assertThat(StockState.of(preorder, stock)).isEqualTo(expected);
	}
}
