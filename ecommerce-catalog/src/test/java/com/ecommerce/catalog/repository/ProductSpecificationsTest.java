package com.ecommerce.catalog.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSpecificationsTest {

	@Test
	void likeWildcardsInSearchTextAreLiteral() {
		assertThat(ProductSpecifications.escape("100%_off\\")).isEqualTo("100\\%\\_off\\\\");
	}
}
