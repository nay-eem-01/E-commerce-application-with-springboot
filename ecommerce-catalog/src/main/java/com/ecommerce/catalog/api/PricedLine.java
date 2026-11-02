package com.ecommerce.catalog.api;

import com.ecommerce.catalog.enums.StockState;

import java.math.BigDecimal;

/**
 * A line priced from the catalog at this moment. Callers snapshot these values onto orders
 * so later price changes never alter an existing order. Prices never come from the client.
 */
public record PricedLine(
		Long productId,
		Long optionId,
		String slug,
		String name,
		String optionName,
		String optionValue,
		BigDecimal unitPrice,
		int quantity,
		boolean preorder,
		BigDecimal depositPerUnit,
		String preorderRelease,
		int availableStock,
		StockState stockState,
		String thumbnailUrl
) {
	public boolean enoughStock() {
		return availableStock >= quantity;
	}
}
