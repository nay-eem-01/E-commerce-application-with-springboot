package com.ecommerce.cart.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartLineRequest {

	@NotNull(message = "Choose a product.")
	private Long productId;

	private Long optionId;

	/** The new quantity for this line. 0 removes it. */
	@Min(value = 0, message = "Quantity cannot be negative.")
	@Max(value = 10, message = "You can buy up to 10 of one item per order.")
	private int quantity;
}
