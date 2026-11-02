package com.ecommerce.order.dto.response;

import com.ecommerce.catalog.enums.StockState;

import java.math.BigDecimal;
import java.util.List;

/** What checkout will charge, computed by the server. The client never computes the total it pays. */
public record QuoteResponse(
		List<Line> lines,
		BigDecimal subtotal,
		String district,
		BigDecimal deliveryFee,
		BigDecimal total,
		BigDecimal depositTotal,
		BigDecimal freeDeliveryThreshold,
		BigDecimal amountToFreeDelivery,
		String deliveryEstimate,
		boolean allInStock
) {
	public record Line(
			Long productId,
			Long optionId,
			String slug,
			String name,
			String optionLabel,
			BigDecimal unitPrice,
			int quantity,
			BigDecimal lineTotal,
			boolean preorder,
			String preorderRelease,
			BigDecimal depositPerUnit,
			StockState stockState,
			boolean enoughStock,
			String thumbnailUrl
	) {}
}
