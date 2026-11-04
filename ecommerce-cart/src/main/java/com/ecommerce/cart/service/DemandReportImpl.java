package com.ecommerce.cart.service;

import com.ecommerce.cart.api.DemandReport;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/** Aggregate SQL over the cart's own tables. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemandReportImpl implements DemandReport {

	private final JdbcTemplate jdbc;

	@Override
	public Map<Long, ProductDemand> byProduct() {
		Map<Long, ProductDemand> out = new HashMap<>();
		jdbc.query("""
				SELECT coalesce(w.product_id, c.product_id), coalesce(w.n, 0), coalesce(c.units, 0), coalesce(c.members, 0)
				FROM (SELECT product_id, count(*) AS n FROM wishlist_items GROUP BY product_id) w
				FULL JOIN (SELECT product_id, sum(quantity) AS units, count(DISTINCT user_id) AS members
				           FROM cart_items GROUP BY product_id) c ON c.product_id = w.product_id
				""", rs -> {
			long id = rs.getLong(1);
			out.put(id, new ProductDemand(id, rs.getLong(2), rs.getLong(3), rs.getLong(4)));
		});
		return out;
	}
}
