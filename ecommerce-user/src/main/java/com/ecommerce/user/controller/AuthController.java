package com.ecommerce.user.controller;

import com.ecommerce.common.config.CommonApiResponses;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
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
import com.ecommerce.user.service.ShopperAuthService;
import com.ecommerce.user.service.ShopperAuthService.Link;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Shopper email and Google sign-in, staff password sign-in, and sessions")
@RestController
@CommonApiResponses
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final ShopperAuthService shopperAuth;

	public record EmailRequest(@NotBlank @Email @Size(max = 254) String email) {
		@Override public String toString() { return "EmailRequest[email=masked]"; }
	}

	public record EmailTokenRequest(@NotBlank @Size(max = 128) String token,
									@NotBlank @Size(max = 64) String password) {
		@Override public String toString() { return "EmailTokenRequest[redacted]"; }
	}

	public record GoogleRequest(@NotBlank @Size(max = 4096) String idToken) {
		@Override public String toString() { return "GoogleRequest[redacted]"; }
	}

	@Operation(summary = "Send a link to finish creating a shopper account.")
	@PostMapping("/register")
	public ResponseEntity<Response> register(@Valid @RequestBody EmailRequest request, HttpServletRequest http) {
		shopperAuth.emailLink(request.email(), Link.REGISTER, ClientIp.of(http));
		return Response.ok("If you can register with this address, we sent you a link.");
	}

	@Operation(summary = "Verify the shopper email and set a password.")
	@PostMapping("/verify-email")
	public ResponseEntity<Response> verifyEmail(@Valid @RequestBody EmailTokenRequest request, HttpServletRequest http) {
		return Response.ok("You are signed in.", shopperAuth.finishEmail(request.token(), request.password(),
				Link.REGISTER, ClientIp.of(http), http.getHeader(HttpHeaders.USER_AGENT)));
	}

	@Operation(summary = "Send a password reset link to an eligible shopper.")
	@PostMapping("/password/forgot")
	public ResponseEntity<Response> forgotPassword(@Valid @RequestBody EmailRequest request, HttpServletRequest http) {
		shopperAuth.emailLink(request.email(), Link.RESET, ClientIp.of(http));
		return Response.ok("If your account is eligible, we sent you a reset link.");
	}

	@Operation(summary = "Use the emailed link to set a new shopper password.")
	@PostMapping("/password/reset")
	public ResponseEntity<Response> resetPassword(@Valid @RequestBody EmailTokenRequest request, HttpServletRequest http) {
		return Response.ok("You are signed in.", shopperAuth.finishEmail(request.token(), request.password(),
				Link.RESET, ClientIp.of(http), http.getHeader(HttpHeaders.USER_AGENT)));
	}

	@Operation(summary = "Sign in with a verified Google Identity Services credential.")
	@PostMapping("/google")
	public ResponseEntity<Response> google(@Valid @RequestBody GoogleRequest request, HttpServletRequest http) {
		return Response.ok("You are signed in.", shopperAuth.signInWithGoogle(request.idToken(), ClientIp.of(http),
				http.getHeader(HttpHeaders.USER_AGENT)));
	}
	private final SessionService sessionService;
	private final CurrentUser currentUser;
	private final Environment environment;

	private void requireDevelopmentSms() {
		if (!environment.matchesProfiles("development", "test")) {
			throw ResponseException.notFound(ErrorCode.SMS_SIGN_IN_DISABLED);
		}
	}

	@Operation(summary = "Text a 6-digit sign-in code to a mobile number. Creates no account yet.")
	@PostMapping("/otp/request")
	public ResponseEntity<Response> requestOtp(@Valid @RequestBody OtpRequest request, HttpServletRequest http) {
		requireDevelopmentSms();
		return Response.ok("We sent a 6-digit code by SMS.", authService.requestOtp(request.getPhone(), ClientIp.of(http)));
	}

	@Operation(summary = "Check the code and sign in. Creates a customer account on first sign-in.")
	@PostMapping("/otp/verify")
	public ResponseEntity<Response> verifyOtp(@Valid @RequestBody OtpVerifyRequest request, HttpServletRequest http) {
		requireDevelopmentSms();
		return Response.ok("You are signed in.",
				authService.verifyOtp(request, ClientIp.of(http), http.getHeader(HttpHeaders.USER_AGENT)));
	}

	@Operation(summary = "Sign in with email and password.")
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
