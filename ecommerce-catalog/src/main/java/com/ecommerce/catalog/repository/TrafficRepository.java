package com.ecommerce.catalog.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/** Daily view and search counters (V10). Plain SQL: the rows are counters, not entities. */
@Repository
@RequiredArgsConstructor
public class TrafficRepository {

	private final JdbcTemplate jdbc;

	public record ViewRow(LocalDate day, long productId, long views) {}

	public record SearchRow(LocalDate day, String term, long searches, long zeroResults) {}

	/** Adds to the day's counts; a product deleted meanwhile is skipped. */
	public void addViews(List<ViewRow> rows) {
		jdbc.batchUpdate("""
				INSERT INTO product_view_daily (day, product_id, views)
				SELECT ?, p.id, ? FROM products p WHERE p.id = ?
				ON CONFLICT (day, product_id) DO UPDATE SET views = product_view_daily.views + EXCLUDED.views
				""", rows, rows.size(), (ps, r) -> {
			ps.setDate(1, Date.valueOf(r.day()));
			ps.setLong(2, r.views());
			ps.setLong(3, r.productId());
		});
	}

	public void addSearches(List<SearchRow> rows) {
		jdbc.batchUpdate("""
				INSERT INTO search_term_daily (day, term, searches, zero_results) VALUES (?, ?, ?, ?)
				ON CONFLICT (day, term) DO UPDATE SET searches = search_term_daily.searches + EXCLUDED.searches,
				    zero_results = search_term_daily.zero_results + EXCLUDED.zero_results
				""", rows, rows.size(), (ps, r) -> {
			ps.setDate(1, Date.valueOf(r.day()));
			ps.setString(2, r.term());
			ps.setLong(3, r.searches());
			ps.setLong(4, r.zeroResults());
		});
	}

	public void deleteBefore(LocalDate day) {
		jdbc.update("DELETE FROM product_view_daily WHERE day < ?", Date.valueOf(day));
		jdbc.update("DELETE FROM search_term_daily WHERE day < ?", Date.valueOf(day));
	}
}
