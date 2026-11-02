package com.ecommerce.catalog.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/** Set the stock of a product (no options) or of one option. */
@Getter
@Setter
public class StockUpdateRequest {

	private Long optionId;

	@Min(value = 0, message = "Stock cannot be negative.")
	@Max(value = 1_000_000, message = "Stock is too large.")
	private int stock;
}
