package com.ecommerce.insights.service;

import com.ecommerce.catalog.api.CatalogReport.ProductSnapshot;
import com.ecommerce.insights.config.InsightsProperties;
import com.ecommerce.insights.dto.response.ProductInsightResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

final class Fixtures {
	private Fixtures() {}

	static final InsightsProperties PROPS = new InsightsProperties(14, 3, 50, new BigDecimal("0.01"), 3, 30, 2000);
	static final Period NOV = new Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
	static final Instant OLD = Instant.parse("2026-01-01T00:00:00Z");

	static ProductSnapshot snapshot(long id, int stock) {
		return new ProductSnapshot(id, "p-" + id, "Product " + id, "Bottles", "Kitsune", true, false, stock, OLD);
	}

	/** A row with the given numbers; conversion and days left are derived the way the service does it. */
	static ProductInsightResponse row(long id, int stock, long views, long orders, long units, long wishlists, long cartMembers,
									  boolean preorder) {
		return new ProductInsightResponse(id, "p-" + id, "Product " + id, "Bottles", "Kitsune", preorder, stock, views, orders,
				units, BigDecimal.valueOf(units * 100L), InsightsService.conversion(orders, views), wishlists, cartMembers,
				cartMembers, InsightsService.daysLeft(stock, units, NOV.days()), List.of());
	}
}
