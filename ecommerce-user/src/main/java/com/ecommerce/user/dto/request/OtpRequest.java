package com.ecommerce.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpRequest {

	@NotBlank(message = "Enter your mobile number.")
	@Size(max = 20, message = "Enter your mobile number.")
	private String phone;
}
