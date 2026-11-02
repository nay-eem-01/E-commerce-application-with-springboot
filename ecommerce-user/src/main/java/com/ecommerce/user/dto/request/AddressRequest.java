package com.ecommerce.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {

	@NotBlank(message = "Enter the name the rider should ask for.")
	@Size(min = 2, max = 80, message = "Use 2 to 80 characters for the name.")
	private String fullName;

	@NotBlank(message = "Enter a mobile number so the rider can reach you.")
	@Size(max = 20, message = "Use an 11-digit mobile number starting with 01, like 01712345678.")
	private String phone;

	@NotBlank(message = "Choose a district.")
	@Size(max = 40, message = "Choose a district.")
	private String district;

	@NotBlank(message = "Enter your area or thana.")
	@Size(max = 80, message = "Use at most 80 characters for the area.")
	private String area;

	@NotBlank(message = "Add a house and road number so the rider can find you.")
	@Size(min = 5, max = 250, message = "Use 5 to 250 characters for the address.")
	private String addressLine;

	private boolean defaultAddress;
}
