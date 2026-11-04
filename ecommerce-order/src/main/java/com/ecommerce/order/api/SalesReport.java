package com.ecommerce.order.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only sales numbers for the insights dashboard. {@code from} is inclusive, {@code to} exclusive.
 * A counted order is one placed in the window that is not CANCELLED or RETURNED; revenue is its subtotal.
 */
public interface SalesReport {

	record SalesTotals(long orders, long units, BigDecimal revenue, BigDecimal deliveryFees,
					   long cancelled, long returned, long newBuyers, long returningBuyers) {}

	/** Shop (Dhaka) day; only days that had counted orders. */
	record DailySales(LocalDate day, long orders, BigDecimal revenue) {}

	/** Revenue here is the sum of line totals. */
	record ProductSales(Long productId, long orders, long units, BigDecimal revenue) {}

	record GroupSales(String key, long orders, long units, BigDecimal revenue) {}

	SalesTotals totals(Instant from, Instant to);

	List<DailySales> daily(Instant from, Instant to);

	List<ProductSales> byProduct(Instant from, Instant to);

	List<GroupSales> byDistrict(Instant from, Instant to);

	List<GroupSales> byPaymentMethod(Instant from, Instant to);
}
