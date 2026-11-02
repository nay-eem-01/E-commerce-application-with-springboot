package com.ecommerce.review.service;

import com.ecommerce.catalog.api.CatalogApi;
import com.ecommerce.common.constant.AppConstants;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.model.PageResponse;
import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.common.web.Pages;
import com.ecommerce.order.api.PurchaseVerifier;
import com.ecommerce.review.dto.request.ReviewRequest;
import com.ecommerce.review.dto.response.ReviewPageResponse;
import com.ecommerce.review.dto.response.ReviewResponse;
import com.ecommerce.review.entity.Review;
import com.ecommerce.review.enums.ReviewStatus;
import com.ecommerce.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReviewService {

	static final Set<String> SORTS = Set.of(AppConstants.SORT_BY_VALUE, "rating");

	private final ReviewRepository reviewRepository;
	private final CatalogApi catalogApi;
	private final PurchaseVerifier purchaseVerifier;
	private final CurrentUser currentUser;

	@Transactional(readOnly = true)
	public ReviewPageResponse list(String slug, int pageNo, int pageSize, String sortBy, AscOrDescType dir) {
		Long productId = productId(slug);
		Map<Integer, Long> distribution = new LinkedHashMap<>();
		for (int stars = 5; stars >= 1; stars--) {
			distribution.put(stars, 0L);
		}
		long count = 0;
		long sum = 0;
		for (Object[] row : reviewRepository.ratingCounts(productId, ReviewStatus.PUBLISHED)) {
			int stars = (Integer) row[0];
			long n = (Long) row[1];
			distribution.put(stars, n);
			count += n;
			sum += stars * n;
		}
		BigDecimal average = count == 0 ? BigDecimal.ZERO.setScale(1)
				: BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
		Long viewer = currentUser.idOrNull();
		PageResponse<ReviewResponse> page = PageResponse.of(reviewRepository.findByProductIdAndStatus(productId,
				ReviewStatus.PUBLISHED, Pages.of(pageNo, pageSize, sortBy, dir, SORTS)), r -> ReviewResponse.from(r, viewer));
		return new ReviewPageResponse(new ReviewPageResponse.Summary(average, count, distribution), page);
	}

	/** Only after the product was delivered to the caller; one review each. */
	@Transactional
	public ReviewResponse create(String slug, Long userId, String displayName, ReviewRequest request) {
		Long productId = productId(slug);
		if (!purchaseVerifier.hasReceived(userId, productId)) {
			throw ResponseException.forbidden(ErrorCode.REVIEW_NOT_PURCHASED);
		}
		if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
			throw ResponseException.conflict(ErrorCode.REVIEW_EXISTS);
		}
		Review review = new Review();
		review.setProductId(productId);
		review.setUserId(userId);
		review.setRating(request.getRating());
		review.setBody(request.getBody().trim());
		review.setAuthorName(displayName);
		return ReviewResponse.from(reviewRepository.save(review), userId);
	}

	/** Owner edits their own review. Reviews are public, so not-yours is 403, not 404. */
	@Transactional
	public ReviewResponse update(Long reviewId, ReviewRequest request) {
		Review review = find(reviewId);
		currentUser.assertOwnerOrAdmin(review.getUserId());
		review.setRating(request.getRating());
		review.setBody(request.getBody().trim());
		return ReviewResponse.from(review, currentUser.idOrNull());
	}

	@Transactional
	public void delete(Long reviewId) {
		Review review = find(reviewId);
		currentUser.assertOwnerOrAdmin(review.getUserId());
		reviewRepository.delete(review);
	}

	@Transactional
	public void moderate(Long reviewId, ReviewStatus status) {
		find(reviewId).setStatus(status);
	}

	/** "Tasnim Rahman" -> "Tasnim R."; no name -> "Verified buyer". */
	public static String displayName(String fullName) {
		if (fullName == null || fullName.isBlank()) {
			return "Verified buyer";
		}
		String[] parts = fullName.trim().split("\\s+");
		String first = parts[0].length() > 30 ? parts[0].substring(0, 30) : parts[0];
		return parts.length > 1 ? first + " " + parts[parts.length - 1].charAt(0) + "." : first;
	}

	private Long productId(String slug) {
		return catalogApi.activeProductIdBySlug(slug).orElseThrow(() -> ResponseException.notFound(ErrorCode.PRODUCT_NOT_FOUND));
	}

	private Review find(Long id) {
		return reviewRepository.findById(id).orElseThrow(() -> ResponseException.notFound(ErrorCode.REVIEW_NOT_FOUND));
	}
}
