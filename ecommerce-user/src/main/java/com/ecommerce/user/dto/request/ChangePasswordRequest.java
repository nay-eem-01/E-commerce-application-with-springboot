package com.ecommerce.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

	@NotBlank(message = "Enter your current password.")
	@Size(max = 64, message = "Your current password is wrong.")
	private String currentPassword;

	@NotBlank(message = "Enter a new password.")
	@Size(max = 64, message = "Use 8 to 64 characters for your password.")
	private String newPassword;

	@Override
	public String toString() {
		return "ChangePasswordRequest[***]";
	}
}
