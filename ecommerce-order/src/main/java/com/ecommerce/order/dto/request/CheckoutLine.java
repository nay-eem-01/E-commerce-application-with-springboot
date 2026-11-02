package com.ecommerce.order.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Ids and a quantity only. Any price the client sends is ignored by design - there is no field for it. */
@Getter
@Setter
public class CheckoutLine {

	@NotNull(message = "Choose a product.")
	private Long productId;

	private Long optionId;

	@Min(value = 1, message = "Quantity must be at least 1.")
	@Max(value = 10, message = "You can buy up to 10 of one item per order.")
	private int quantity;
}
