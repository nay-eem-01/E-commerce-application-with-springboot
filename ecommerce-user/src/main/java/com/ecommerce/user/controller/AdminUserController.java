package com.ecommerce.user.controller;

import com.ecommerce.common.config.CommonApiResponses;
import com.ecommerce.common.constant.AppConstants;
import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.model.Response;
import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.user.dto.request.UpdateUserStatusRequest;
import com.ecommerce.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: users")
@RestController
@CommonApiResponses
@SecurityRequirement(name = "jwtToken")
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

	private final UserService userService;
	private final CurrentUser currentUser;

	@Operation(summary = "Search users by name, phone or email. Paginated.")
	@PreAuthorize("hasAuthority('USER_READ')")
	@GetMapping
	public ResponseEntity<Response> list(
			@RequestParam(required = false) String q,
			@RequestParam(name = AppConstants.PAGE_NO, defaultValue = AppConstants.PAGE_NO_VALUE) int pageNo,
			@RequestParam(name = AppConstants.PAGE_SIZE, defaultValue = AppConstants.PAGE_SIZE_VALUE) int pageSize,
			@RequestParam(name = AppConstants.SORT_BY, defaultValue = AppConstants.SORT_BY_VALUE) String sortBy,
			@RequestParam(name = AppConstants.ASC_OR_DESC, defaultValue = AppConstants.ASC_OR_DESC_VALUE) AscOrDescType ascOrDesc) {
		return Response.ok("Users loaded.", userService.search(q, pageNo, pageSize, sortBy, ascOrDesc));
	}

	@Operation(summary = "Suspend or restore a user. Suspending signs them out everywhere.")
	@PreAuthorize("hasAuthority('USER_UPDATE')")
	@PutMapping("/{userId}/status")
	public ResponseEntity<Response> updateStatus(@PathVariable Long userId, @Valid @RequestBody UpdateUserStatusRequest request) {
		return Response.ok("User status updated.", userService.updateStatus(currentUser.id(), userId, request.getStatus()));
	}
}
