package com.ecommerce.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpVerifyRequest {

	@NotBlank(message = "Enter your mobile number.")
	@Size(max = 20, message = "Enter your mobile number.")
	private String phone;

	@NotBlank(message = "Enter the 6-digit code from the SMS.")
	@Pattern(regexp = "^\\d{6}$", message = "Enter the 6-digit code from the SMS.")
	private String code;

	/** Optional: only used when this sign-in creates the account. */
	@Size(max = 80, message = "Use at most 80 characters for your name.")
	private String name;

	/** Optional, stored truncated: lets a shopper recognise the device in a session list later. */
	@Size(max = 200)
	private String deviceLabel;

	/** Never log the code. */
	@Override
	public String toString() {
		return "OtpVerifyRequest[phone=masked, code=***]";
	}
}
