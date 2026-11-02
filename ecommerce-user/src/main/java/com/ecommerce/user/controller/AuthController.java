package com.ecommerce.user.controller;

import com.ecommerce.common.config.CommonApiResponses;
import com.ecommerce.common.model.Response;
import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.common.web.ClientIp;
import com.ecommerce.user.dto.request.OtpRequest;
import com.ecommerce.user.dto.request.OtpVerifyRequest;
import com.ecommerce.user.dto.request.RefreshTokenRequest;
import com.ecommerce.user.dto.request.SignInRequest;
import com.ecommerce.user.security.UserPrincipal;
import com.ecommerce.user.service.AuthService;
import com.ecommerce.user.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Phone-code sign-in for shoppers, password sign-in for staff, token refresh")
@RestController
@CommonApiResponses
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final SessionService sessionService;
	private final CurrentUser currentUser;

	@Operation(summary = "Text a 6-digit sign-in code to a mobile number. Creates no account yet.")
	@PostMapping("/otp/request")
	public ResponseEntity<Response> requestOtp(@Valid @RequestBody OtpRequest request, HttpServletRequest http) {
		return Response.ok("We sent a 6-digit code by SMS.", authService.requestOtp(request.getPhone(), ClientIp.of(http)));
	}

	@Operation(summary = "Check the code and sign in. Creates a customer account on first sign-in.")
	@PostMapping("/otp/verify")
	public ResponseEntity<Response> verifyOtp(@Valid @RequestBody OtpVerifyRequest request, HttpServletRequest http) {
		return Response.ok("You are signed in.",
				authService.verifyOtp(request, ClientIp.of(http), http.getHeader(HttpHeaders.USER_AGENT)));
	}

	@Operation(summary = "Staff sign-in with email and password.")
	@PostMapping("/sign-in")
	public ResponseEntity<Response> signIn(@Valid @RequestBody SignInRequest request, HttpServletRequest http) {
		return Response.ok("You are signed in.",
				authService.signIn(request, ClientIp.of(http), http.getHeader(HttpHeaders.USER_AGENT)));
	}

	@Operation(summary = "Swap a refresh token for a new access token and a new refresh token. Each refresh token works once.")
	@PostMapping("/refresh-token")
	public ResponseEntity<Response> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return Response.ok("Session refreshed.", sessionService.refresh(request.getRefreshToken()));
	}

	/** /api/auth/** is public at the filter level, so this resolves the caller itself. */
	@Operation(summary = "Sign out this device.", security = @SecurityRequirement(name = "jwtToken"))
	@PostMapping("/sign-out")
	public ResponseEntity<Response> signOut() {
		UserPrincipal principal = (UserPrincipal) currentUser.require();
		sessionService.signOut(principal.getSessionId());
		return Response.ok("You are signed out.");
	}
}
