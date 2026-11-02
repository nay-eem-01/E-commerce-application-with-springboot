package com.ecommerce.cart.dto.response;

import com.ecommerce.catalog.enums.StockState;

import java.math.BigDecimal;
import java.util.List;

/**
 * The cart priced live from the catalog. Delivery is not included here: it depends on the
 * address and is quoted by POST /api/checkout/quote.
 */
public record CartResponse(List<Line> items, int itemCount, BigDecimal subtotal) {

	public record Line(
			Long itemId,
			Long productId,
			Long optionId,
			String slug,
			String name,
			String optionName,
			String optionValue,
			BigDecimal unitPrice,
			int quantity,
			BigDecimal lineTotal,
			StockState stockState,
			/** False when the product was archived or the option removed; it is excluded from the subtotal. */
			boolean available,
			boolean preorder,
			String preorderRelease,
			String thumbnailUrl
	) {}
}
