package com.ecommerce.order.controller;

import com.ecommerce.common.config.CommonApiResponses;
import com.ecommerce.common.constant.AppConstants;
import com.ecommerce.common.model.Response;
import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "My orders", description = "The signed-in shopper's orders")
@RestController
@CommonApiResponses
@SecurityRequirement(name = "jwtToken")
@RequestMapping("/api/orders/me")
@RequiredArgsConstructor
public class MyOrderController {

	private final OrderService orderService;
	private final CurrentUser currentUser;

	@Operation(summary = "My orders, newest first. Paginated.")
	@GetMapping
	public ResponseEntity<Response> myOrders(
			@RequestParam(name = AppConstants.PAGE_NO, defaultValue = AppConstants.PAGE_NO_VALUE) int pageNo,
			@RequestParam(name = AppConstants.PAGE_SIZE, defaultValue = AppConstants.PAGE_SIZE_VALUE) int pageSize) {
		return Response.ok("Orders loaded.", orderService.myOrders(currentUser.id(), pageNo, pageSize));
	}

	@Operation(summary = "One of my orders with its delivery timeline.")
	@GetMapping("/{orderNumber}")
	public ResponseEntity<Response> myOrder(@PathVariable String orderNumber) {
		return Response.ok("Order loaded.", orderService.myOrder(currentUser.id(), orderNumber));
	}

	@Operation(summary = "Cancel one of my orders. Only possible before it is packed.")
	@PostMapping("/{orderNumber}/cancel")
	public ResponseEntity<Response> cancel(@PathVariable String orderNumber) {
		return Response.ok("Order cancelled.", orderService.cancelMine(currentUser.id(), orderNumber));
	}
}
