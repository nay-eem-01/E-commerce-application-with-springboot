package com.ecommerce.review.dto.response;

import com.ecommerce.common.model.PageResponse;

import java.math.BigDecimal;
import java.util.Map;

public record ReviewPageResponse(Summary summary, PageResponse<ReviewResponse> reviews) {

	/** distribution maps stars (1-5) to how many reviews gave that many. */
	public record Summary(BigDecimal average, long count, Map<Integer, Long> distribution) {}
}
