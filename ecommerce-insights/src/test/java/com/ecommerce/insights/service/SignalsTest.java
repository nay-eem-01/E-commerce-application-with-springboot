package com.ecommerce.insights.service;

import com.ecommerce.catalog.api.CatalogReport.EarlyAccessCount;
import com.ecommerce.catalog.api.CatalogReport.SearchTermCount;
import com.ecommerce.insights.dto.response.ProductInsightResponse;
import com.ecommerce.insights.dto.response.SignalResponse;
import com.ecommerce.insights.enums.SignalType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.ecommerce.insights.service.Fixtures.NOV;
import static com.ecommerce.insights.service.Fixtures.OLD;
import static com.ecommerce.insights.service.Fixtures.PROPS;
import static com.ecommerce.insights.service.Fixtures.row;
import static org.assertj.core.api.Assertions.assertThat;

class SignalsTest {

	private final Signals signals = new Signals(PROPS);

	private List<SignalType> of(ProductInsightResponse r) {
		return signals.forProduct(r, OLD, NOV);
	}

	@Test
	void restockSoonAt14DaysNotAt15() {
		// 30 days, 30 units sold = 1 a day
		assertThat(of(row(1, 14, 0, 30, 30, 0, 0, false))).contains(SignalType.RESTOCK_SOON);
		assertThat(of(row(1, 15, 0, 30, 30, 0, 0, false))).doesNotContain(SignalType.RESTOCK_SOON);
		assertThat(of(row(1, 14, 0, 30, 30, 0, 0, true))).doesNotContain(SignalType.RESTOCK_SOON);
		assertThat(of(row(1, 0, 0, 30, 30, 0, 0, false))).doesNotContain(SignalType.RESTOCK_SOON);
	}

	@Test
	void wantedSoldOutNeedsThreeAndNotPreorder() {
		assertThat(of(row(1, 0, 0, 0, 0, 2, 1, false))).contains(SignalType.WANTED_SOLD_OUT);
		assertThat(of(row(1, 0, 0, 0, 0, 1, 1, false))).doesNotContain(SignalType.WANTED_SOLD_OUT);
		assertThat(of(row(1, 0, 0, 0, 0, 3, 0, true))).doesNotContain(SignalType.WANTED_SOLD_OUT);
	}

	@Test
	void lowStockHighDemandWhenWantedByAtLeastTheStock() {
		assertThat(of(row(1, 3, 0, 0, 0, 2, 1, false))).contains(SignalType.LOW_STOCK_HIGH_DEMAND);
		assertThat(of(row(1, 4, 0, 0, 0, 2, 1, false))).doesNotContain(SignalType.LOW_STOCK_HIGH_DEMAND);
		assertThat(of(row(1, 0, 0, 0, 0, 2, 1, false))).doesNotContain(SignalType.LOW_STOCK_HIGH_DEMAND);
	}

	@Test
	void lookedNotBoughtAt50ViewsUnder1Percent() {
		assertThat(of(row(1, 10, 50, 0, 0, 0, 0, false))).contains(SignalType.LOOKED_NOT_BOUGHT);
		assertThat(of(row(1, 10, 49, 0, 0, 0, 0, false))).doesNotContain(SignalType.LOOKED_NOT_BOUGHT);
		assertThat(of(row(1, 10, 100, 1, 1, 0, 0, false))).doesNotContain(SignalType.LOOKED_NOT_BOUGHT); // exactly 1 %
		assertThat(of(row(1, 10, 101, 1, 1, 0, 0, false))).contains(SignalType.LOOKED_NOT_BOUGHT);
	}

	@Test
	void slowMoverOnlyIfOlderThanPeriod() {
		assertThat(of(row(1, 10, 0, 0, 0, 0, 0, false))).contains(SignalType.SLOW_MOVER);
		assertThat(signals.forProduct(row(1, 10, 0, 0, 0, 0, 0, false), Instant.parse("2026-11-10T00:00:00Z"), NOV))
				.doesNotContain(SignalType.SLOW_MOVER);
		assertThat(of(row(1, 10, 0, 0, 0, 0, 0, true))).doesNotContain(SignalType.SLOW_MOVER);
		assertThat(of(row(1, 10, 0, 1, 1, 0, 0, false))).doesNotContain(SignalType.SLOW_MOVER);
	}

	@Test
	void searchGapAtThreeSearches() {
		List<SignalResponse> out = signals.build(List.of(), List.of(new SearchTermCount("naruto", 4, 3),
				new SearchTermCount("goku", 2, 2)), List.of());
		assertThat(out).extracting(SignalResponse::term).containsExactly("naruto");
		assertThat(out.getFirst().reason()).isEqualTo("\"naruto\" was searched 3 times and found nothing.");
	}

	@Test
	void comingSoonNeedsOneSignup() {
		List<SignalResponse> out = signals.build(List.of(), List.of(),
				List.of(new EarlyAccessCount("apparel", "Apparel", 1), new EarlyAccessCount("cosplay", "Cosplay", 0)));
		assertThat(out).extracting(SignalResponse::categorySlug).containsExactly("apparel");
	}

	@Test
	void orderedByTypeThenRankAndCutAt30() {
		List<Signals.Candidate> rows = new ArrayList<>();
		IntStream.range(0, 40).forEach(i -> rows.add(new Signals.Candidate(row(i, 10 + i, 0, 0, 0, 0, 0, false), OLD, NOV)));
		rows.add(new Signals.Candidate(row(99, 5, 0, 30, 30, 0, 0, false), OLD, NOV)); // restock soon
		List<SignalResponse> out = signals.build(rows, List.of(), List.of());
		assertThat(out).hasSize(30);
		assertThat(out.getFirst().type()).isEqualTo(SignalType.RESTOCK_SOON);
		assertThat(out.get(1).productId()).isEqualTo(39L); // slow movers, most stock first
	}
}
