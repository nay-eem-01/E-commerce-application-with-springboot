package com.ecommerce.review.dto.response;

import com.ecommerce.review.entity.Review;

import java.time.Instant;

/** Public review: no user id, no contact details. Every review is from a verified buyer. */
public record ReviewResponse(Long id, int rating, String body, String authorName, boolean verifiedBuyer, Instant creationDate, boolean mine) {

	public static ReviewResponse from(Review r, Long viewerId) {
		return new ReviewResponse(r.getId(), r.getRating(), r.getBody(), r.getAuthorName(), true, r.getCreationDate(),
				viewerId != null && viewerId.equals(r.getUserId()));
	}
}
