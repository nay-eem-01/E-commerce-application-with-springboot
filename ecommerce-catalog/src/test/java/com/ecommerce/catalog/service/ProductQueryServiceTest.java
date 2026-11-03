package com.ecommerce.catalog.service;

import com.ecommerce.common.exception.ResponseException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductQueryServiceTest {

	@Test
	void tagFiltersGroupByTheirGroup() {
		Map<String, List<String>> groups = ProductQueryService.tagGroups(List.of("Capacity:750 ml", "Capacity: 1 L", "Material:Steel"));
		assertThat(groups).containsExactly(
				Map.entry("Capacity", List.of("750 ml", "1 L")),
				Map.entry("Material", List.of("Steel")));
	}

	@Test
	void noTagsMeansNoFilter() {
		assertThat(ProductQueryService.tagGroups(null)).isEmpty();
		assertThat(ProductQueryService.tagGroups(List.of())).isEmpty();
	}

	@Test
	void aMalformedTagIsRejectedNotIgnored() {
		assertThatThrownBy(() -> ProductQueryService.tagGroups(List.of("Steel"))).isInstanceOf(ResponseException.class);
		assertThatThrownBy(() -> ProductQueryService.tagGroups(List.of(":Steel"))).isInstanceOf(ResponseException.class);
		assertThatThrownBy(() -> ProductQueryService.tagGroups(List.of("Material:"))).isInstanceOf(ResponseException.class);
	}

	@Test
	void measuredFilterValuesSortBySize() {
		List<String> values = new java.util.ArrayList<>(List.of("1 L", "750 ml", "500 ml", "600 ml"));
		values.sort(ProductQueryService::compareAmounts);
		assertThat(values).containsExactly("500 ml", "600 ml", "750 ml", "1 L");
	}

	@Test
	void unmeasuredValuesKeepTheirOrder() {
		List<String> values = new java.util.ArrayList<>(List.of("S", "M", "L", "XL"));
		values.sort(ProductQueryService::compareAmounts);
		assertThat(values).containsExactly("S", "M", "L", "XL");
		assertThat(ProductQueryService.compareAmounts("Steel", "Tritan")).isZero();
	}
}
