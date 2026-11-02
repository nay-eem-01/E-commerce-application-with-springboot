package com.ecommerce.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignInRequest {

	@NotBlank(message = "Enter your email.")
	@Email(message = "Enter a valid email.")
	@Size(max = 254, message = "Enter a valid email.")
	private String email;

	@NotBlank(message = "Enter your password.")
	@Size(max = 64, message = "Invalid email or password.")
	private String password;

	@Override
	public String toString() {
		return "SignInRequest[email=masked, password=***]";
	}
}
