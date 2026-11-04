package com.ecommerce.order.service;

import com.ecommerce.order.api.SalesReport;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** Aggregate SQL over the order module's own tables. Buyers are told apart by the order's phone number; only counts leave. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesReportImpl implements SalesReport {

	private static final String COUNTED = "o.status NOT IN ('CANCELLED', 'RETURNED')";
	private static final String IN_WINDOW = "o.placed_at >= ? AND o.placed_at < ?";

	private final JdbcTemplate jdbc;

	@Override
	public SalesTotals totals(Instant from, Instant to) {
		OffsetDateTime f = utc(from), t = utc(to);
		long units = jdbc.queryForObject("SELECT coalesce(sum(i.quantity), 0) FROM order_items i JOIN orders o ON o.id = i.order_id WHERE "
				+ COUNTED + " AND " + IN_WINDOW, Long.class, f, t);
		long[] buyers = jdbc.queryForObject("""
				WITH firsts AS (SELECT o.phone, min(o.placed_at) AS first_at FROM orders o WHERE %s GROUP BY o.phone)
				SELECT count(*) FILTER (WHERE f.first_at >= ?), count(*) FILTER (WHERE f.first_at < ?)
				FROM (SELECT DISTINCT o.phone FROM orders o WHERE %s AND %s) b JOIN firsts f ON f.phone = b.phone
				""".formatted(COUNTED, COUNTED, IN_WINDOW), (rs, i) -> new long[]{rs.getLong(1), rs.getLong(2)}, f, f, f, t);
		return jdbc.queryForObject("""
				SELECT count(*) FILTER (WHERE %1$s), coalesce(sum(o.subtotal) FILTER (WHERE %1$s), 0),
				       coalesce(sum(o.delivery_fee) FILTER (WHERE %1$s), 0),
				       count(*) FILTER (WHERE o.status = 'CANCELLED'), count(*) FILTER (WHERE o.status = 'RETURNED')
				FROM orders o WHERE %2$s
				""".formatted(COUNTED, IN_WINDOW), (rs, i) -> new SalesTotals(rs.getLong(1), units, rs.getBigDecimal(2),
				rs.getBigDecimal(3), rs.getLong(4), rs.getLong(5), buyers[0], buyers[1]), f, t);
	}

	@Override
	public List<DailySales> daily(Instant from, Instant to) {
		return jdbc.query("""
				SELECT (o.placed_at AT TIME ZONE 'Asia/Dhaka')::date AS day, count(*), sum(o.subtotal)
				FROM orders o WHERE %s AND %s GROUP BY day ORDER BY day
				""".formatted(COUNTED, IN_WINDOW),
				(rs, i) -> new DailySales(rs.getObject(1, java.time.LocalDate.class), rs.getLong(2), rs.getBigDecimal(3)),
				utc(from), utc(to));
	}

	@Override
	public List<ProductSales> byProduct(Instant from, Instant to) {
		return jdbc.query("""
				SELECT i.product_id, count(DISTINCT o.id), sum(i.quantity), sum(i.line_total)
				FROM order_items i JOIN orders o ON o.id = i.order_id WHERE %s AND %s GROUP BY i.product_id
				""".formatted(COUNTED, IN_WINDOW),
				(rs, i) -> new ProductSales(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getBigDecimal(4)), utc(from), utc(to));
	}

	@Override
	public List<GroupSales> byDistrict(Instant from, Instant to) {
		return grouped("o.district", from, to);
	}

	@Override
	public List<GroupSales> byPaymentMethod(Instant from, Instant to) {
		return grouped("o.payment_method", from, to);
	}

	/** {@code column} is one of this class's constants, never input. */
	private List<GroupSales> grouped(String column, Instant from, Instant to) {
		return jdbc.query("""
				SELECT %1$s, count(*), sum(u.units), sum(o.subtotal)
				FROM orders o JOIN (SELECT order_id, sum(quantity) AS units FROM order_items GROUP BY order_id) u ON u.order_id = o.id
				WHERE %2$s AND %3$s GROUP BY %1$s ORDER BY sum(o.subtotal) DESC
				""".formatted(column, COUNTED, IN_WINDOW),
				(rs, i) -> new GroupSales(rs.getString(1), rs.getLong(2), rs.getLong(3), rs.getBigDecimal(4)), utc(from), utc(to));
	}

	private static OffsetDateTime utc(Instant instant) {
		return instant.atOffset(ZoneOffset.UTC);
	}
}
