package com.ecommerce.catalog.dto.response;

import com.ecommerce.catalog.entity.Product;

import java.util.Comparator;
import java.util.List;

/** Exact stock for staff (the shop's own responses show a count only when it is low). */
public record StockResponse(Long productId, int stock, List<OptionStock> options) {

	public record OptionStock(Long id, String value, int stock) {}

	public static StockResponse from(Product p) {
		return new StockResponse(p.getId(), p.getStock(), p.getOptions().stream()
				.sorted(Comparator.comparingInt(o -> o.getSortOrder()))
				.map(o -> new OptionStock(o.getId(), o.getValue(), o.getStock())).toList());
	}
}
