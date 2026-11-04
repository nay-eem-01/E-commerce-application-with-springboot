package com.ecommerce.insights.service;

import com.ecommerce.cart.api.DemandReport;
import com.ecommerce.cart.api.DemandReport.ProductDemand;
import com.ecommerce.catalog.api.CatalogReport;
import com.ecommerce.catalog.api.CatalogReport.ProductSnapshot;
import com.ecommerce.catalog.api.CatalogReport.TrafficTotals;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.insights.dto.response.BreakdownRowResponse;
import com.ecommerce.insights.dto.response.OverviewResponse;
import com.ecommerce.insights.dto.response.ProductInsightResponse;
import com.ecommerce.insights.enums.BreakdownBy;
import com.ecommerce.order.api.SalesReport;
import com.ecommerce.order.api.SalesReport.DailySales;
import com.ecommerce.order.api.SalesReport.ProductSales;
import com.ecommerce.order.api.SalesReport.SalesTotals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.ecommerce.insights.service.Fixtures.NOV;
import static com.ecommerce.insights.service.Fixtures.OLD;
import static com.ecommerce.insights.service.Fixtures.PROPS;
import static com.ecommerce.insights.service.Fixtures.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InsightsServiceTest {

	private final CatalogReport catalog = mock(CatalogReport.class);
	private final DemandReport demand = mock(DemandReport.class);
	private final SalesReport sales = mock(SalesReport.class);
	private final InsightsService service = new InsightsService(catalog, demand, sales, new Signals(PROPS), PROPS);

	private static final SalesTotals NONE = new SalesTotals(0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0, 0);

	@BeforeEach
	void empty() {
		when(sales.totals(any(), any())).thenReturn(NONE);
		when(catalog.totals(any(), any())).thenReturn(new TrafficTotals(0, 0, 0));
		when(catalog.products()).thenReturn(List.of());
		when(catalog.views(any(), any())).thenReturn(Map.of());
		when(demand.byProduct()).thenReturn(Map.of());
		when(sales.byProduct(any(), any())).thenReturn(List.of());
		when(sales.daily(any(), any())).thenReturn(List.of());
		when(catalog.topZeroResultSearches(any(), any(), anyInt())).thenReturn(List.of());
		when(catalog.earlyAccess()).thenReturn(List.of());
	}

	@Test
	void emptyPeriodGivesZerosAndNulls() {
		when(catalog.products()).thenReturn(List.of(snapshot(1, 5)));
		OverviewResponse o = service.overview(NOV);
		assertThat(o.current().orders()).isZero();
		assertThat(o.current().averageOrder()).isNull();
		assertThat(o.current().cancelRate()).isEqualByComparingTo("0");
		assertThat(o.daily()).hasSize(30).allMatch(d -> d.orders() == 0);

		ProductInsightResponse row = service.products(NOV, "revenue", AscOrDescType.desc).getFirst();
		assertThat(row.conversion()).isNull();
		assertThat(row.daysLeft()).isNull();
		assertThat(service.breakdown(NOV, BreakdownBy.CATEGORY)).isEmpty();
	}

	@Test
	void ratesAndAveragesAreRounded() {
		when(sales.totals(any(), any())).thenReturn(new SalesTotals(3, 4, new BigDecimal("1000.00"), new BigDecimal("180.00"), 1, 0, 2, 1));
		var t = service.overview(NOV).current();
		assertThat(t.averageOrder()).isEqualByComparingTo("333.33");
		assertThat(t.cancelRate()).isEqualByComparingTo("0.2500");
	}

	@Test
	void salesOfArchivedProductCountInTotalsOnly() {
		ProductSnapshot archived = new ProductSnapshot(2L, "old", "Old bottle", "Bottles", null, false, false, 0, OLD);
		when(catalog.products()).thenReturn(List.of(snapshot(1, 5), archived));
		when(sales.byProduct(any(), any())).thenReturn(List.of(new ProductSales(2L, 1, 2, new BigDecimal("500.00")),
				new ProductSales(77L, 1, 1, new BigDecimal("100.00"))));

		assertThat(service.products(NOV, "revenue", AscOrDescType.desc)).extracting(ProductInsightResponse::id).containsExactly(1L);
		assertThat(service.signals(NOV)).noneMatch(s -> Long.valueOf(2L).equals(s.productId()));
		assertThat(service.breakdown(NOV, BreakdownBy.CATEGORY)).extracting(BreakdownRowResponse::name)
				.containsExactly("Bottles", "Unknown");
	}

	@Test
	void dailySeriesFillsMissingDays() {
		when(sales.daily(any(), any())).thenReturn(List.of(new DailySales(LocalDate.of(2026, 11, 3), 2, new BigDecimal("900.00"))));
		List<DailySales> daily = service.overview(NOV).daily();
		assertThat(daily).hasSize(30);
		assertThat(daily.get(2)).isEqualTo(new DailySales(LocalDate.of(2026, 11, 3), 2, new BigDecimal("900.00")));
		assertThat(daily.get(3).revenue()).isEqualByComparingTo("0");
	}

	@Test
	void breakdownByCharacterRollsUpAndNamesNoCharacter() {
		ProductSnapshot plain = new ProductSnapshot(2L, "plain", "Plain bag", "Bags", null, true, false, 3, OLD);
		when(catalog.products()).thenReturn(List.of(snapshot(1, 5), snapshot(3, 5), plain));
		when(sales.byProduct(any(), any())).thenReturn(List.of(new ProductSales(1L, 2, 2, new BigDecimal("200.00")),
				new ProductSales(3L, 1, 1, new BigDecimal("100.00")), new ProductSales(2L, 1, 1, new BigDecimal("100.00"))));

		List<BreakdownRowResponse> rows = service.breakdown(NOV, BreakdownBy.CHARACTER);
		assertThat(rows).extracting(BreakdownRowResponse::name).containsExactly("Kitsune", "No character");
		assertThat(rows.getFirst().revenue()).isEqualByComparingTo("300.00");
		assertThat(rows.getFirst().share()).isEqualByComparingTo("0.7500");
	}

	@Test
	void productsSortByDaysLeftPutsNullsLast() {
		when(catalog.products()).thenReturn(List.of(snapshot(1, 5), snapshot(2, 30), snapshot(3, 9)));
		when(sales.byProduct(any(), any())).thenReturn(List.of(new ProductSales(2L, 30, 30, new BigDecimal("1.00")),
				new ProductSales(3L, 30, 30, new BigDecimal("1.00"))));
		assertThat(service.products(NOV, "daysLeft", AscOrDescType.asc)).extracting(ProductInsightResponse::id).containsExactly(3L, 2L, 1L);
		assertThat(service.products(NOV, "daysLeft", AscOrDescType.desc)).extracting(ProductInsightResponse::id).containsExactly(2L, 3L, 1L);
	}

	@Test
	void demandIsMergedIntoTheRow() {
		when(catalog.products()).thenReturn(List.of(snapshot(1, 5)));
		when(catalog.views(any(), any())).thenReturn(Map.of(1L, 200L));
		when(demand.byProduct()).thenReturn(Map.of(1L, new ProductDemand(1L, 4, 3, 2)));
		when(sales.byProduct(any(), any())).thenReturn(List.of(new ProductSales(1L, 5, 6, new BigDecimal("600.00"))));
		ProductInsightResponse row = service.products(NOV, "revenue", AscOrDescType.desc).getFirst();
		assertThat(row.views()).isEqualTo(200);
		assertThat(row.conversion()).isEqualByComparingTo("0.0250");
		assertThat(row.wishlists()).isEqualTo(4);
		assertThat(row.cartUnits()).isEqualTo(3);
		assertThat(row.daysLeft()).isEqualByComparingTo("25.0");
	}

	@Test
	void unknownSortIs400() {
		assertThatThrownBy(() -> service.products(NOV, "phone", AscOrDescType.desc))
				.isInstanceOf(ResponseException.class).hasMessage(ErrorCode.INVALID_SORT_FIELD);
	}
}
