package com.ecommerce.catalog.controller;

import com.ecommerce.catalog.dto.request.EarlyAccessRequest;
import com.ecommerce.catalog.service.EarlyAccessService;
import com.ecommerce.common.model.Response;
import com.ecommerce.common.web.ClientIp;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@com.ecommerce.common.config.CommonApiResponses
@RequiredArgsConstructor
public class EarlyAccessController {

	private final EarlyAccessService earlyAccessService;

	@Operation(summary = "Ask to hear first when a coming-soon category opens. Phone, email or both. Public, rate limited.")
	@PostMapping("/api/early-access")
	public ResponseEntity<Response> signUp(@Valid @RequestBody EarlyAccessRequest request, HttpServletRequest http) {
		earlyAccessService.signUp(request, ClientIp.of(http));
		return Response.ok("You are on the list. We will tell you first.");
	}

	@Operation(summary = "Early-access signups, newest first, optionally for one category.")
	@PreAuthorize("hasAuthority('USER_READ')")
	@GetMapping("/api/admin/early-access")
	public ResponseEntity<Response> list(@RequestParam(required = false) String category,
										 @RequestParam(defaultValue = "0") int pageNo,
										 @RequestParam(defaultValue = "50") int pageSize) {
		return Response.ok("Signups loaded.", earlyAccessService.list(category, pageNo, pageSize));
	}
}
