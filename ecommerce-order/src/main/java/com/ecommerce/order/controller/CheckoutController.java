package com.ecommerce.order.controller;

import com.ecommerce.common.config.CommonApiResponses;
import com.ecommerce.common.constant.AppConstants;
import com.ecommerce.common.model.Response;
import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.common.web.ClientIp;
import com.ecommerce.order.dto.request.PlaceOrderRequest;
import com.ecommerce.order.dto.request.QuoteRequest;
import com.ecommerce.order.dto.request.RetryPaymentRequest;
import com.ecommerce.order.dto.request.TrackOrderRequest;
import com.ecommerce.order.service.CheckoutFacade;
import com.ecommerce.order.service.CheckoutService;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Public: an account is never required to buy. A signed-in caller's order is linked to them. */
@Tag(name = "Checkout", description = "Delivery charges, server-side quote, placing an order, guest tracking")
@RestController
@CommonApiResponses
@RequiredArgsConstructor
public class CheckoutController {

	private final CheckoutService checkoutService;
	private final CheckoutFacade checkoutFacade;
	private final OrderService orderService;
	private final CurrentUser currentUser;

	@Operation(summary = "Districts, delivery charges, free-delivery threshold, delivery times and payment methods.")
	@GetMapping("/api/checkout/delivery-options")
	public ResponseEntity<Response> deliveryOptions() {
		return Response.ok("Delivery options loaded.", checkoutService.deliveryOptions());
	}

	@Operation(summary = "Price a cart for a district: lines, subtotal, delivery fee, total and stock warnings. Places nothing.")
	@PostMapping("/api/checkout/quote")
	public ResponseEntity<Response> quote(@Valid @RequestBody QuoteRequest request) {
		return Response.ok("Quote ready.", checkoutService.quote(request));
	}

	@Operation(summary = "Place an order as a guest or signed in. Send a fresh Idempotency-Key per checkout; a retry with the same key returns the same order. For an online method, paymentAction.url is the provider page to redirect to.")
	@PostMapping("/api/orders")
	public ResponseEntity<Response> place(
			@Parameter(description = "Random 16-64 characters (a UUID is fine), new for each checkout attempt")
			@RequestHeader(name = AppConstants.IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
			@Valid @RequestBody PlaceOrderRequest request, HttpServletRequest http) {
		return Response.created("Order placed.",
				checkoutFacade.placeAndPay(request, idempotencyKey, currentUser.idOrNull(), ClientIp.of(http)));
	}

	@Operation(summary = "Try paying an unpaid online order again (order number + the phone it was placed with). Returns where to redirect.")
	@PostMapping("/api/orders/pay")
	public ResponseEntity<Response> retryPayment(@Valid @RequestBody RetryPaymentRequest request, HttpServletRequest http) {
		return Response.ok("Payment started.", orderService.retryPayment(request.getOrderNumber(), request.getPhone(), ClientIp.of(http)));
	}

	@Operation(summary = "Track an order with its number and the mobile number used to place it. No address or contact details are returned.")
	@PostMapping("/api/orders/track")
	public ResponseEntity<Response> track(@Valid @RequestBody TrackOrderRequest request, HttpServletRequest http) {
		return Response.ok("Order found.", orderService.track(request.getOrderNumber(), request.getPhone(), ClientIp.of(http)));
	}
}
