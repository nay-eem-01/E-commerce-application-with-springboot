package com.ecommerce.payment.controller;

import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.model.Response;
import com.ecommerce.common.ratelimit.RateLimiter;
import com.ecommerce.common.web.ClientIp;
import com.ecommerce.payment.config.PaymentProperties;
import com.ecommerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Map;

/**
 * Public by necessity: vendors call these without our token. Nothing here is trusted - the
 * service confirms every callback with the vendor's server. The shopper is always sent to the
 * configured storefront URL (never a URL taken from the request), so there is no open redirect.
 */
@Tag(name = "Payment callbacks", description = "Where payment providers send the shopper back and post their notifications")
@RestController
@RequestMapping("/api/payments/{provider}")
@RequiredArgsConstructor
public class PaymentCallbackController {

	private final PaymentService paymentService;
	private final PaymentProperties properties;
	private final RateLimiter rateLimiter;

	@Operation(summary = "Browser return from the provider (GET or form POST). Verifies with the provider, then redirects to the storefront result page.")
	@RequestMapping(value = "/callback", method = {RequestMethod.GET, RequestMethod.POST})
	public ResponseEntity<Void> callback(@PathVariable String provider, @RequestParam Map<String, String> params, HttpServletRequest http) {
		UriComponentsBuilder target = UriComponentsBuilder.fromUriString(properties.returnUrl());
		try {
			rateLimiter.hit("payment-callback:ip:" + ClientIp.of(http), 60, Duration.ofMinutes(1));
			PaymentService.CallbackResult result = paymentService.handleCallback(provider, params, "CALLBACK");
			target.queryParam("order", result.orderNumber()).queryParam("payment", result.status());
		} catch (ResponseException e) {
			target.queryParam("payment", e.getHttpStatus() == HttpStatus.NOT_FOUND ? "UNKNOWN" : "PENDING");
		}
		return ResponseEntity.status(HttpStatus.SEE_OTHER).header(HttpHeaders.LOCATION, target.build().encode().toUriString()).build();
	}

	@Operation(summary = "Server-to-server notification (IPN) from the provider. Verified with the provider before anything changes.")
	@PostMapping("/ipn")
	public ResponseEntity<Response> ipn(@PathVariable String provider, @RequestParam Map<String, String> params, HttpServletRequest http) {
		rateLimiter.hit("payment-ipn:ip:" + ClientIp.of(http), 120, Duration.ofMinutes(1));
		PaymentService.CallbackResult result = paymentService.handleCallback(provider, params, "IPN");
		return Response.ok("Notification received.", Map.of("status", result.status()));
	}
}
