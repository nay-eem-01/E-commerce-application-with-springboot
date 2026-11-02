package com.ecommerce.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMeRequest {

	@NotBlank(message = "Enter your name.")
	@Size(min = 2, max = 80, message = "Use 2 to 80 characters for your name.")
	private String name;

	/** Optional: receipts go here. */
	@Email(message = "Enter a valid email.")
	@Size(max = 254, message = "Enter a valid email.")
	private String email;
}
