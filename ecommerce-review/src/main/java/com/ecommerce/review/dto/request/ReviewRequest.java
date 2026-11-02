package com.ecommerce.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {

	@Min(value = 1, message = "Choose 1 to 5 stars.")
	@Max(value = 5, message = "Choose 1 to 5 stars.")
	private int rating;

	@NotBlank(message = "Write a few words about the product.")
	@Size(min = 10, max = 1000, message = "Use 10 to 1000 characters for your review.")
	private String body;
}
