package com.ecommerce.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class QuoteRequest {

	@NotEmpty(message = "Add at least one item before placing an order.")
	@Size(max = 30, message = "An order can have up to 30 different items.")
	@Valid
	private List<CheckoutLine> items = new ArrayList<>();

	/** Optional; without it the quote assumes delivery inside Dhaka. */
	@Size(max = 40)
	private String district;
}
