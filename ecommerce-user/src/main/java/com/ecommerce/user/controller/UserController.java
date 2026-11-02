package com.ecommerce.user.controller;

import com.ecommerce.common.config.CommonApiResponses;
import com.ecommerce.common.model.Response;
import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.user.dto.request.AddressRequest;
import com.ecommerce.user.dto.request.ChangePasswordRequest;
import com.ecommerce.user.dto.request.UpdateMeRequest;
import com.ecommerce.user.service.AddressService;
import com.ecommerce.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Everything here acts on the caller, taken from the token. No user id in any path or body. */
@Tag(name = "My account", description = "The signed-in shopper's profile, password and address book")
@RestController
@CommonApiResponses
@SecurityRequirement(name = "jwtToken")
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final AddressService addressService;
	private final CurrentUser currentUser;

	@Operation(summary = "My profile.")
	@GetMapping
	public ResponseEntity<Response> me() {
		return Response.ok("Profile loaded.", userService.me(currentUser.id()));
	}

	@Operation(summary = "Update my name and receipt email.")
	@PutMapping
	public ResponseEntity<Response> updateMe(@Valid @RequestBody UpdateMeRequest request) {
		return Response.ok("Profile updated.", userService.updateMe(currentUser.id(), request));
	}

	@Operation(summary = "Change my password (staff accounts). Signs out every device.")
	@PostMapping("/change-password")
	public ResponseEntity<Response> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		userService.changePassword(currentUser.id(), request);
		return Response.ok("Password changed. Please sign in again.");
	}

	@Operation(summary = "My saved addresses, default first.")
	@GetMapping("/addresses")
	public ResponseEntity<Response> addresses() {
		return Response.ok("Addresses loaded.", addressService.list(currentUser.id()));
	}

	@Operation(summary = "Save a new address. The first one becomes the default.")
	@PostMapping("/addresses")
	public ResponseEntity<Response> createAddress(@Valid @RequestBody AddressRequest request) {
		return Response.created("Address saved.", addressService.create(currentUser.id(), request));
	}

	@Operation(summary = "Edit one of my addresses.")
	@PutMapping("/addresses/{addressId}")
	public ResponseEntity<Response> updateAddress(@PathVariable Long addressId, @Valid @RequestBody AddressRequest request) {
		return Response.ok("Address updated.", addressService.update(currentUser.id(), addressId, request));
	}

	@Operation(summary = "Remove one of my addresses.")
	@DeleteMapping("/addresses/{addressId}")
	public ResponseEntity<Response> deleteAddress(@PathVariable Long addressId) {
		addressService.delete(currentUser.id(), addressId);
		return Response.ok("Address removed.");
	}
}
