package com.ecommerce.order.dto.request;

import com.ecommerce.payment.api.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Guest or signed-in checkout - the same five required fields either way (name, mobile,
 * district, area, address). Email and a rider note are optional.
 */
@Getter
@Setter
public class PlaceOrderRequest {

	@NotEmpty(message = "Add at least one item before placing an order.")
	@Size(max = 30, message = "An order can have up to 30 different items.")
	@Valid
	private List<CheckoutLine> items = new ArrayList<>();

	@NotBlank(message = "Enter the name the rider should ask for.")
	@Size(min = 2, max = 80, message = "Use 2 to 80 characters for the name.")
	private String fullName;

	@NotBlank(message = "Enter a mobile number so the rider can reach you.")
	@Size(max = 20, message = "Use an 11-digit mobile number starting with 01, like 01712345678.")
	private String phone;

	@Email(message = "This email looks incomplete. Check for the @ and the domain.")
	@Size(max = 254, message = "This email looks incomplete. Check for the @ and the domain.")
	private String email;

	@NotBlank(message = "Choose a district.")
	@Size(max = 40, message = "Choose a district.")
	private String district;

	@NotBlank(message = "Enter your area or thana.")
	@Size(max = 80, message = "Use at most 80 characters for the area.")
	private String area;

	@NotBlank(message = "Add a house and road number so the rider can find you.")
	@Size(min = 5, max = 250, message = "Add a house and road number so the rider can find you.")
	private String addressLine;

	@Size(max = 200, message = "Use at most 200 characters for the rider note.")
	private String note;

	@NotNull(message = "Choose how you want to pay.")
	private PaymentMethod paymentMethod;

	/** Signed-in only: save this address when the shopper has none saved yet. */
	private boolean saveAddress = true;
}
