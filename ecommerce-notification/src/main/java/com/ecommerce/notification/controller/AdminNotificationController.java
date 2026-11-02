package com.ecommerce.notification.controller;

import com.ecommerce.common.config.CommonApiResponses;
import com.ecommerce.common.constant.AppConstants;
import com.ecommerce.common.model.Response;
import com.ecommerce.notification.entity.OutboxEmail;
import com.ecommerce.notification.service.NotificationAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: notifications", description = "Order emails waiting, sent or failed")
@RestController
@CommonApiResponses
@SecurityRequirement(name = "jwtToken")
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

	private final NotificationAdminService service;

	@Operation(summary = "Queued, sent and failed emails, newest first. Filter by status or order number.")
	@PreAuthorize("hasAuthority('NOTIFICATION_MANAGE')")
	@GetMapping
	public ResponseEntity<Response> list(
			@RequestParam(required = false) OutboxEmail.Status status,
			@RequestParam(required = false) String orderNumber,
			@RequestParam(name = AppConstants.PAGE_NO, defaultValue = AppConstants.PAGE_NO_VALUE) int pageNo,
			@RequestParam(name = AppConstants.PAGE_SIZE, defaultValue = AppConstants.PAGE_SIZE_VALUE) int pageSize) {
		return Response.ok("Notifications loaded.", service.search(status, orderNumber, pageNo, pageSize));
	}

	@Operation(summary = "Send a failed email again.")
	@PreAuthorize("hasAuthority('NOTIFICATION_MANAGE')")
	@PostMapping("/{id}/retry")
	public ResponseEntity<Response> retry(@PathVariable Long id) {
		return Response.ok("Email queued again.", service.retry(id));
	}
}
