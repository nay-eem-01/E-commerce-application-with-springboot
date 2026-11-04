package com.ecommerce.review.dto.response;

import com.ecommerce.review.entity.Review;
import com.ecommerce.review.enums.ReviewStatus;

import java.time.Instant;

/** A review for moderation. The author is the public display name only: no user id or contact details. */
public record AdminReviewResponse(Long id, Long productId, String productName, String authorName, int rating, String body,
								  ReviewStatus status, Instant createdAt) {

	public static AdminReviewResponse from(Review r, String productName) {
		return new AdminReviewResponse(r.getId(), r.getProductId(), productName, r.getAuthorName(), r.getRating(), r.getBody(),
				r.getStatus(), r.getCreationDate());
	}
}
