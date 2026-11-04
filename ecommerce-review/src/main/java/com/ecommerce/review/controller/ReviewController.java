package com.ecommerce.review.controller;

import com.ecommerce.common.config.CommonApiResponses;
import com.ecommerce.common.constant.AppConstants;
import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.model.Response;
import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.review.dto.request.ReviewRequest;
import com.ecommerce.review.dto.request.ReviewStatusRequest;
import com.ecommerce.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.ecommerce.review.enums.ReviewStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reviews", description = "Ratings and reviews from verified buyers")
@RestController
@CommonApiResponses
@RequiredArgsConstructor
public class ReviewController {

	private final ReviewService reviewService;
	private final CurrentUser currentUser;

	@Operation(summary = "A product's rating summary and reviews. Public.")
	@GetMapping("/api/products/{slug}/reviews")
	public ResponseEntity<Response> list(
			@PathVariable String slug,
			@RequestParam(name = AppConstants.PAGE_NO, defaultValue = AppConstants.PAGE_NO_VALUE) int pageNo,
			@RequestParam(name = AppConstants.PAGE_SIZE, defaultValue = "10") int pageSize,
			@RequestParam(name = AppConstants.SORT_BY, defaultValue = AppConstants.SORT_BY_VALUE) String sortBy,
			@RequestParam(name = AppConstants.ASC_OR_DESC, defaultValue = AppConstants.ASC_OR_DESC_VALUE) AscOrDescType ascOrDesc) {
		return Response.ok("Reviews loaded.", reviewService.list(slug, pageNo, pageSize, sortBy, ascOrDesc));
	}

	@Operation(summary = "Review a product you received. One review per product.", security = @SecurityRequirement(name = "jwtToken"))
	@PostMapping("/api/products/{slug}/reviews")
	public ResponseEntity<Response> create(
			@PathVariable String slug, @Valid @RequestBody ReviewRequest request) {
		var user = currentUser.require();
		return Response.created("Thanks for your review.",
				reviewService.create(slug, user.getId(), ReviewService.displayName(user.getDisplayName()), request));
	}

	@Operation(summary = "Edit your review.", security = @SecurityRequirement(name = "jwtToken"))
	@PutMapping("/api/reviews/{reviewId}")
	public ResponseEntity<Response> update(@PathVariable Long reviewId, @Valid @RequestBody ReviewRequest request) {
		return Response.ok("Review updated.", reviewService.update(reviewId, request));
	}

	@Operation(summary = "Delete your review.", security = @SecurityRequirement(name = "jwtToken"))
	@DeleteMapping("/api/reviews/{reviewId}")
	public ResponseEntity<Response> delete(@PathVariable Long reviewId) {
		reviewService.delete(reviewId);
		return Response.ok("Review deleted.");
	}

	@Operation(summary = "Reviews for moderation, newest first; filter by status and product.", security = @SecurityRequirement(name = "jwtToken"))
	@PreAuthorize("hasAuthority('REVIEW_MODERATE')")
	@GetMapping("/api/admin/reviews")
	public ResponseEntity<Response> adminList(
			@RequestParam(required = false) ReviewStatus status,
			@RequestParam(required = false) Long productId,
			@RequestParam(name = AppConstants.PAGE_NO, defaultValue = AppConstants.PAGE_NO_VALUE) int pageNo,
			@RequestParam(name = AppConstants.PAGE_SIZE, defaultValue = AppConstants.PAGE_SIZE_VALUE) int pageSize) {
		return Response.ok("Reviews loaded.", reviewService.adminList(status, productId, pageNo, pageSize));
	}

	@Operation(summary = "Hide or publish a review (moderation).", security = @SecurityRequirement(name = "jwtToken"))
	@PreAuthorize("hasAuthority('REVIEW_MODERATE')")
	@PatchMapping("/api/admin/reviews/{reviewId}/status")
	public ResponseEntity<Response> moderate(@PathVariable Long reviewId, @Valid @RequestBody ReviewStatusRequest request) {
		reviewService.moderate(reviewId, request.getStatus());
		return Response.ok("Review updated.");
	}
}
