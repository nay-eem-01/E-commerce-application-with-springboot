package com.ecommerce.order.controller;

import com.ecommerce.common.config.CommonApiResponses;
import com.ecommerce.common.constant.AppConstants;
import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.model.Response;
import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.order.dto.request.OrderActionRequest;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: orders")
@RestController
@CommonApiResponses
@SecurityRequirement(name = "jwtToken")
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

	private final OrderService orderService;
	private final CurrentUser currentUser;

	@Operation(summary = "All orders, filtered by status or searched by order number or phone. Paginated.")
	@PreAuthorize("hasAuthority('ORDER_READ_ALL')")
	@GetMapping
	public ResponseEntity<Response> list(
			@RequestParam(required = false) OrderStatus status,
			@RequestParam(required = false) String q,
			@RequestParam(name = AppConstants.PAGE_NO, defaultValue = AppConstants.PAGE_NO_VALUE) int pageNo,
			@RequestParam(name = AppConstants.PAGE_SIZE, defaultValue = AppConstants.PAGE_SIZE_VALUE) int pageSize,
			@RequestParam(name = AppConstants.SORT_BY, defaultValue = "placedAt") String sortBy,
			@RequestParam(name = AppConstants.ASC_OR_DESC, defaultValue = AppConstants.ASC_OR_DESC_VALUE) AscOrDescType ascOrDesc) {
		return Response.ok("Orders loaded.", orderService.search(status, q, pageNo, pageSize, sortBy, ascOrDesc));
	}

	@Operation(summary = "One order with address, contact details and timeline.")
	@PreAuthorize("hasAuthority('ORDER_READ_ALL')")
	@GetMapping("/{orderNumber}")
	public ResponseEntity<Response> get(@PathVariable String orderNumber) {
		return Response.ok("Order loaded.", orderService.adminGet(orderNumber));
	}

	@Operation(summary = "Move an order: PACK, SHIP, DELIVER, CANCEL or RETURN. An illegal move is 409.")
	@PreAuthorize("hasAuthority('ORDER_UPDATE')")
	@PatchMapping("/{orderNumber}/status")
	public ResponseEntity<Response> applyAction(@PathVariable String orderNumber, @Valid @RequestBody OrderActionRequest request) {
		return Response.ok("Order updated.",
				orderService.adminApply(orderNumber, request.getAction(), request.getNote(), currentUser.id()));
	}
}
