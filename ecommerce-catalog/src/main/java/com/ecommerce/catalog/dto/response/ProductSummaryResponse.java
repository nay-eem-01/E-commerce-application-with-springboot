package com.ecommerce.catalog.dto.response;

import com.ecommerce.catalog.entity.Product;
import com.ecommerce.catalog.enums.StockState;

import java.math.BigDecimal;

/** Everything a product card shows: price, the key spec and the stock line. */
public record ProductSummaryResponse(
		Long id,
		String slug,
		String name,
		String categorySlug,
		String specLine,
		BigDecimal price,
		BigDecimal compareAtPrice,
		StockState stockState,
		/** Only set while stock is low ("Only 4 left"); null otherwise, so exact stock is not published. */
		Integer stockLeft,
		String preorderRelease,
		BigDecimal deposit,
		boolean hasOptions,
		String thumbnailUrl
) {
	public static ProductSummaryResponse from(Product p) {
		StockState state = p.stockState();
		return new ProductSummaryResponse(
				p.getId(), p.getSlug(), p.getName(), p.getCategory().getSlug(), p.getSpecLine(),
				p.getPrice(), p.getCompareAtPrice(), state,
				state == StockState.LOW_STOCK ? p.getStock() : null,
				ProductDetailResponse.month(p.getPreorderRelease()), p.isPreorder() ? p.getDeposit() : null,
				p.hasOptions(), p.thumbnailUrl()
		);
	}
}
