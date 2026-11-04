package com.ecommerce.insights.dto.response;

import com.ecommerce.insights.enums.SignalType;

/** One of productId/term/categorySlug is set, by type. {@code value} is the number the signal is ranked by. */
public record SignalResponse(SignalType type, String title, String reason, Long productId, String productSlug,
							 String term, String categorySlug, long value) {}
