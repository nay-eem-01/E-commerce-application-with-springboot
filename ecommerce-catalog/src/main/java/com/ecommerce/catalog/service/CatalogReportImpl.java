package com.ecommerce.catalog.service;

import com.ecommerce.catalog.api.CatalogReport;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Aggregate SQL over the catalog's own tables; reports read rows, not entities, so there is no repository layer here. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogReportImpl implements CatalogReport {

	private final JdbcTemplate jdbc;

	@Override
	public List<ProductSnapshot> products() {
		return jdbc.query("""
				SELECT p.id, p.slug, p.name, c.name AS category, ch.name AS character, p.status, p.preorder, p.stock, p.creation_date
				FROM products p JOIN categories c ON c.id = p.category_id LEFT JOIN characters ch ON ch.id = p.character_id
				""", (rs, i) -> new ProductSnapshot(rs.getLong("id"), rs.getString("slug"), rs.getString("name"),
				rs.getString("category"), rs.getString("character"), "ACTIVE".equals(rs.getString("status")),
				rs.getBoolean("preorder"), rs.getInt("stock"), rs.getTimestamp("creation_date").toInstant()));
	}

	@Override
	public Map<Long, Long> views(LocalDate from, LocalDate to) {
		Map<Long, Long> out = new HashMap<>();
		jdbc.query("SELECT product_id, sum(views) FROM product_view_daily WHERE day BETWEEN ? AND ? GROUP BY product_id",
				rs -> { out.put(rs.getLong(1), rs.getLong(2)); }, Date.valueOf(from), Date.valueOf(to));
		return out;
	}

	@Override
	public TrafficTotals totals(LocalDate from, LocalDate to) {
		long views = jdbc.queryForObject("SELECT coalesce(sum(views), 0) FROM product_view_daily WHERE day BETWEEN ? AND ?",
				Long.class, Date.valueOf(from), Date.valueOf(to));
		return jdbc.queryForObject("""
				SELECT coalesce(sum(searches), 0), coalesce(sum(zero_results), 0) FROM search_term_daily WHERE day BETWEEN ? AND ?
				""", (rs, i) -> new TrafficTotals(views, rs.getLong(1), rs.getLong(2)), Date.valueOf(from), Date.valueOf(to));
	}

	@Override
	public List<SearchTermCount> topSearches(LocalDate from, LocalDate to, int limit) {
		return terms("", "searches", from, to, limit);
	}

	@Override
	public List<SearchTermCount> topZeroResultSearches(LocalDate from, LocalDate to, int limit) {
		return terms("HAVING sum(zero_results) > 0", "zero_results", from, to, limit);
	}

	private List<SearchTermCount> terms(String having, String orderBy, LocalDate from, LocalDate to, int limit) {
		// having and orderBy are constants from this class, never input
		return jdbc.query("SELECT term, sum(searches) AS searches, sum(zero_results) AS zero_results FROM search_term_daily"
						+ " WHERE day BETWEEN ? AND ? GROUP BY term " + having + " ORDER BY " + orderBy + " DESC, term LIMIT ?",
				(rs, i) -> new SearchTermCount(rs.getString("term"), rs.getLong("searches"), rs.getLong("zero_results")),
				Date.valueOf(from), Date.valueOf(to), limit);
	}

	@Override
	public List<EarlyAccessCount> earlyAccess() {
		return jdbc.query("""
				SELECT c.slug, c.name, count(s.id) FROM categories c LEFT JOIN early_access_signups s ON s.category_id = c.id
				WHERE c.coming_soon AND c.active GROUP BY c.id, c.slug, c.name ORDER BY count(s.id) DESC, c.name
				""", (rs, i) -> new EarlyAccessCount(rs.getString(1), rs.getString(2), rs.getLong(3)));
	}
}
