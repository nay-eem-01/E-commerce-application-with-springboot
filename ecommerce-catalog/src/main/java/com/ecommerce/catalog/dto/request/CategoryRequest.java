package com.ecommerce.catalog.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {

	@NotBlank(message = "Enter a slug.")
	@Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Use lowercase letters, numbers and single hyphens for the slug.")
	@Size(max = 60, message = "Use at most 60 characters for the slug.")
	private String slug;

	@NotBlank(message = "Enter a name.")
	@Size(max = 60, message = "Use at most 60 characters for the name.")
	private String name;

	@Size(max = 8, message = "Use at most 8 characters for the label.")
	private String label;

	@Size(max = 160, message = "Use at most 160 characters for the blurb.")
	private String blurb;

	@Min(value = 0, message = "Sort order cannot be negative.")
	@Max(value = 10_000, message = "Sort order is too large.")
	private int sortOrder;

	private boolean active = true;
}
