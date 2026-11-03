package com.ecommerce.catalog.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** A phone number, an email, or both. */
@Getter
@Setter
public class EarlyAccessRequest {

	@NotBlank(message = "Choose what you want early access to.")
	@Size(max = 60, message = "Choose what you want early access to.")
	private String categorySlug;

	@Size(max = 20, message = "Use an 11-digit mobile number starting with 01, like 01712345678.")
	private String phone;

	@Email(message = "This email looks incomplete. Check for the @ and the domain.")
	@Size(max = 254, message = "This email looks incomplete. Check for the @ and the domain.")
	private String email;

	@Override
	public String toString() {
		return "EarlyAccessRequest[categorySlug=" + categorySlug + ", contact=masked]";
	}
}
