package com.ecommerce.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {

	@NotBlank(message = "Your session has ended. Please sign in again.")
	@Size(max = 100, message = "Your session has ended. Please sign in again.")
	private String refreshToken;

	@Override
	public String toString() {
		return "RefreshTokenRequest[refreshToken=***]";
	}
}
