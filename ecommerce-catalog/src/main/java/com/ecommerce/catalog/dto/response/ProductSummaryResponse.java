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
		String thumbnailUrl,
		/** The character collection's slug, or null. */
		String characterSlug,
		/** Listed in the last 30 days. */
		boolean newArrival,
		/** Colour options, for the dots on a card and quick add; empty when options are not colours. */
		java.util.List<Swatch> swatches,
		/** A set of other products (card tag "Set"). */
		boolean set
) {
	public record Swatch(Long optionId, String value, String swatch, StockState stockState) {}

	public static ProductSummaryResponse from(Product p) {
		StockState state = p.stockState();
		return new ProductSummaryResponse(
				p.getId(), p.getSlug(), p.getName(), p.getCategory().getSlug(), p.getSpecLine(),
				p.getPrice(), p.getCompareAtPrice(), state,
				state == StockState.LOW_STOCK ? p.getStock() : null,
				ProductDetailResponse.month(p.getPreorderRelease()), p.isPreorder() ? p.getDeposit() : null,
				p.hasOptions(), p.thumbnailUrl(),
				p.getCharacter() == null ? null : p.getCharacter().getSlug(),
				p.isNewArrival(java.time.Instant.now()),
				p.getOptions().stream().filter(o -> o.getSwatch() != null)
						.map(o -> new Swatch(o.getId(), o.getValue(), o.getSwatch(), StockState.of(p.isPreorder(), o.getStock())))
						.toList(),
				p.isSet()
		);
	}
}
