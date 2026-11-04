package com.ecommerce.insights.dto.response;

import com.ecommerce.insights.enums.SignalType;

import java.math.BigDecimal;
import java.util.List;

/** {@code conversion} = orders / views (null without views); {@code daysLeft} null when nothing sold. */
public record ProductInsightResponse(Long id, String slug, String name, String category, String character, boolean preorder,
									 int stock, long views, long orders, long units, BigDecimal revenue, BigDecimal conversion,
									 long wishlists, long cartUnits, long cartMembers, BigDecimal daysLeft,
									 List<SignalType> signals) {

	public ProductInsightResponse withSignals(List<SignalType> s) {
		return new ProductInsightResponse(id, slug, name, category, character, preorder, stock, views, orders, units, revenue,
				conversion, wishlists, cartUnits, cartMembers, daysLeft, s);
	}
}
