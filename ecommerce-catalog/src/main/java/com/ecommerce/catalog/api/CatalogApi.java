package com.ecommerce.catalog.api;

import com.ecommerce.catalog.dto.response.ProductSummaryResponse;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The catalog's public surface for other modules (cart, order, review). They call this,
 * never a catalog repository or entity.
 */
public interface CatalogApi {

	/** Prices lines against active products. 400 for an unknown option or a missing option choice; 404 for an unknown or inactive product. */
	List<PricedLine> price(List<LineRequest> lines);

	/** Takes stock for every line or none; 409 OUT_OF_STOCK naming the product ids that fell short. */
	void reserve(List<LineRequest> lines);

	/** Puts stock back (cancelled order). */
	void release(List<LineRequest> lines);

	/** Summaries for display, in no particular order; inactive products are skipped. */
	List<ProductSummaryResponse> summaries(Collection<Long> productIds);

	Optional<Long> activeProductIdBySlug(String slug);
}
