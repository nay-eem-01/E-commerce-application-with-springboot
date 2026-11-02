package com.ecommerce.cart.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** The guest cart from the browser, sent once right after sign-in. */
@Getter
@Setter
public class CartMergeRequest {

	@NotNull
	@Valid
	@Size(max = 50, message = "Your cart can hold up to 50 different items.")
	private List<CartLineRequest> items = new ArrayList<>();
}
