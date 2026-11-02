package com.ecommerce.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrackOrderRequest {

	@NotBlank(message = "Enter your order number, like SS-7K2M9QXA.")
	@Size(max = 16, message = "Enter your order number, like SS-7K2M9QXA.")
	private String orderNumber;

	@NotBlank(message = "Enter the mobile number you ordered with.")
	@Size(max = 20, message = "Enter the mobile number you ordered with.")
	private String phone;
}
