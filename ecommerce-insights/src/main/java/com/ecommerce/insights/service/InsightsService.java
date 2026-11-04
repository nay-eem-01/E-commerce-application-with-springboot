package com.ecommerce.insights.service;

import com.ecommerce.cart.api.DemandReport;
import com.ecommerce.cart.api.DemandReport.ProductDemand;
import com.ecommerce.catalog.api.CatalogReport;
import com.ecommerce.catalog.api.CatalogReport.ProductSnapshot;
import com.ecommerce.catalog.api.CatalogReport.TrafficTotals;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.insights.config.InsightsProperties;
import com.ecommerce.insights.dto.response.BreakdownRowResponse;
import com.ecommerce.insights.dto.response.OverviewResponse;
import com.ecommerce.insights.dto.response.ProductInsightResponse;
import com.ecommerce.insights.dto.response.SearchesResponse;
import com.ecommerce.insights.dto.response.SignalResponse;
import com.ecommerce.insights.dto.response.TotalsResponse;
import com.ecommerce.insights.enums.BreakdownBy;
import com.ecommerce.order.api.SalesReport;
import com.ecommerce.order.api.SalesReport.DailySales;
import com.ecommerce.order.api.SalesReport.GroupSales;
import com.ecommerce.order.api.SalesReport.ProductSales;
import com.ecommerce.order.api.SalesReport.SalesTotals;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Merges the catalog, cart and order reports into the dashboard's numbers. Has no tables of its own. */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsightsService {

	static final int TERM_LIMIT = 20;
	private static final ProductDemand NO_DEMAND = new ProductDemand(null, 0, 0, 0);

	private static final Map<String, Comparator<ProductInsightResponse>> SORTS = Map.of(
			"revenue", Comparator.comparing(ProductInsightResponse::revenue),
			"units", Comparator.comparingLong(ProductInsightResponse::units),
			"views", Comparator.comparingLong(ProductInsightResponse::views),
			"conversion", nullsLast(ProductInsightResponse::conversion),
			"wishlists", Comparator.comparingLong(ProductInsightResponse::wishlists),
			"daysLeft", nullsLast(ProductInsightResponse::daysLeft),
			"stock", Comparator.comparingInt(ProductInsightResponse::stock));

	private final CatalogReport catalog;
	private final DemandReport demand;
	private final SalesReport sales;
	private final Signals signals;
	private final InsightsProperties props;

	public OverviewResponse overview(Period p) {
		Map<LocalDate, DailySales> byDay = sales.daily(p.start(), p.endExclusive()).stream()
				.collect(Collectors.toMap(DailySales::day, Function.identity()));
		List<DailySales> daily = p.from().datesUntil(p.to().plusDays(1))
				.map(d -> byDay.getOrDefault(d, new DailySales(d, 0, BigDecimal.ZERO))).toList();
		return new OverviewResponse(p.from(), p.to(), totals(p), totals(p.previous()), daily);
	}

	/** Every active product, sorted; nulls (no views, nothing sold) last in either direction. */
	public List<ProductInsightResponse> products(Period p, String sort, AscOrDescType dir) {
		Comparator<ProductInsightResponse> cmp = SORTS.get(sort);
		if (cmp == null) throw ResponseException.badRequest(ErrorCode.INVALID_SORT_FIELD);
		if (dir == AscOrDescType.desc) cmp = reversedKeepingNullsLast(sort, cmp);
		return candidates(p).stream().map(c -> c.row().withSignals(signals.forProduct(c.row(), c.createdAt(), p)))
				.sorted(cmp.thenComparing(ProductInsightResponse::name)).toList();
	}

	public List<SignalResponse> signals(Period p) {
		return signals.build(candidates(p), catalog.topZeroResultSearches(p.from(), p.to(), TERM_LIMIT), catalog.earlyAccess());
	}

	public List<BreakdownRowResponse> breakdown(Period p, BreakdownBy by) {
		List<GroupSales> groups = switch (by) {
			case DISTRICT -> sales.byDistrict(p.start(), p.endExclusive());
			case PAYMENT_METHOD -> sales.byPaymentMethod(p.start(), p.endExclusive());
			case CATEGORY -> rollUp(p, s -> s.category());
			case CHARACTER -> rollUp(p, s -> s.character() == null ? "No character" : s.character());
		};
		BigDecimal total = groups.stream().map(GroupSales::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
		return groups.stream().sorted(Comparator.comparing(GroupSales::revenue).reversed())
				.map(g -> new BreakdownRowResponse(g.key(), g.orders(), g.units(), g.revenue(), ratio(g.revenue(), total)))
				.toList();
	}

	public SearchesResponse searches(Period p) {
		return new SearchesResponse(catalog.topSearches(p.from(), p.to(), TERM_LIMIT),
				catalog.topZeroResultSearches(p.from(), p.to(), TERM_LIMIT));
	}

	/** orders / views; null without views, and null when orders outnumber views (views were missed, so the ratio means nothing). */
	static BigDecimal conversion(long orders, long views) {
		return views == 0 || orders > views ? null : BigDecimal.valueOf(orders).divide(BigDecimal.valueOf(views), 4, RoundingMode.HALF_UP);
	}

	/** stock / (units per day), one decimal; null when nothing sold. */
	static BigDecimal daysLeft(int stock, long units, int days) {
		return units == 0 ? null
				: BigDecimal.valueOf((long) stock * days).divide(BigDecimal.valueOf(units), 1, RoundingMode.HALF_UP);
	}

	private TotalsResponse totals(Period p) {
		SalesTotals s = sales.totals(p.start(), p.endExclusive());
		TrafficTotals t = catalog.totals(p.from(), p.to());
		BigDecimal average = s.orders() == 0 ? null
				: s.revenue().divide(BigDecimal.valueOf(s.orders()), 2, RoundingMode.HALF_UP);
		BigDecimal cancelRate = ratio(BigDecimal.valueOf(s.cancelled()), BigDecimal.valueOf(s.orders() + s.cancelled()));
		return new TotalsResponse(s.orders(), s.units(), s.revenue(), average, s.deliveryFees(), s.cancelled(), cancelRate,
				s.returned(), s.newBuyers(), s.returningBuyers(), t.views(), t.searches(), t.zeroResultSearches());
	}

	private List<Signals.Candidate> candidates(Period p) {
		Map<Long, ProductSales> sold = sales.byProduct(p.start(), p.endExclusive()).stream()
				.collect(Collectors.toMap(ProductSales::productId, Function.identity()));
		Map<Long, Long> views = catalog.views(p.from(), p.to());
		Map<Long, ProductDemand> wanted = demand.byProduct();
		List<ProductSnapshot> active = catalog.products().stream().filter(ProductSnapshot::active).toList();
		if (active.size() > props.productLimit()) {
			// shortcut: merged in memory; past app.insights.product-limit products move paging and sorting into SQL.
			log.warn("Insights product table cut to {} of {} active products", props.productLimit(), active.size());
			active = active.subList(0, props.productLimit());
		}
		List<Signals.Candidate> out = new ArrayList<>(active.size());
		for (ProductSnapshot s : active) {
			ProductSales ps = sold.getOrDefault(s.id(), new ProductSales(s.id(), 0, 0, BigDecimal.ZERO));
			ProductDemand d = wanted.getOrDefault(s.id(), NO_DEMAND);
			long v = views.getOrDefault(s.id(), 0L);
			ProductInsightResponse row = new ProductInsightResponse(s.id(), s.slug(), s.name(), s.category(), s.character(),
					s.preorder(), s.stock(), v, ps.orders(), ps.units(), ps.revenue(), conversion(ps.orders(), v),
					d.wishlists(), d.cartUnits(), d.cartMembers(), daysLeft(s.stock(), ps.units(), p.days()), List.of());
			out.add(new Signals.Candidate(row, s.createdAt(), p));
		}
		return out;
	}

	/**
	 * Category and character rows from per-product sales. An order holding two products of the same group counts
	 * once per product in {@code orders}; units and revenue are exact.
	 */
	private List<GroupSales> rollUp(Period p, Function<ProductSnapshot, String> key) {
		Map<Long, ProductSnapshot> products = catalog.products().stream()
				.collect(Collectors.toMap(ProductSnapshot::id, Function.identity()));
		Map<String, long[]> counts = new LinkedHashMap<>();
		Map<String, BigDecimal> revenue = new LinkedHashMap<>();
		for (ProductSales s : sales.byProduct(p.start(), p.endExclusive())) {
			ProductSnapshot snap = products.get(s.productId());
			String name = snap == null ? "Unknown" : key.apply(snap);
			long[] c = counts.computeIfAbsent(name, k -> new long[2]);
			c[0] += s.orders();
			c[1] += s.units();
			revenue.merge(name, s.revenue(), BigDecimal::add);
		}
		return counts.entrySet().stream()
				.map(e -> new GroupSales(e.getKey(), e.getValue()[0], e.getValue()[1], revenue.get(e.getKey()))).toList();
	}

	private static BigDecimal ratio(BigDecimal part, BigDecimal whole) {
		return whole.signum() == 0 ? BigDecimal.ZERO.setScale(4) : part.divide(whole, 4, RoundingMode.HALF_UP);
	}

	private static Comparator<ProductInsightResponse> nullsLast(Function<ProductInsightResponse, BigDecimal> key) {
		return Comparator.comparing(key, Comparator.nullsLast(Comparator.naturalOrder()));
	}

	private static Comparator<ProductInsightResponse> reversedKeepingNullsLast(String sort, Comparator<ProductInsightResponse> cmp) {
		return switch (sort) {
			case "conversion" -> Comparator.comparing(ProductInsightResponse::conversion, Comparator.nullsLast(Comparator.reverseOrder()));
			case "daysLeft" -> Comparator.comparing(ProductInsightResponse::daysLeft, Comparator.nullsLast(Comparator.reverseOrder()));
			default -> cmp.reversed();
		};
	}
}
