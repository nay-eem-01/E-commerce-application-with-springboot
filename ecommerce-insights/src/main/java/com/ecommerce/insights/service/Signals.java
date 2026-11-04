package com.ecommerce.insights.service;

import com.ecommerce.catalog.api.CatalogReport.EarlyAccessCount;
import com.ecommerce.catalog.api.CatalogReport.SearchTermCount;
import com.ecommerce.insights.config.InsightsProperties;
import com.ecommerce.insights.dto.response.ProductInsightResponse;
import com.ecommerce.insights.dto.response.SignalResponse;
import com.ecommerce.insights.enums.SignalType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The signal rules. Pure: numbers in, signals out. */
@Component
@RequiredArgsConstructor
public class Signals {

	/** A product row with what the rules need beyond it. */
	public record Candidate(ProductInsightResponse row, Instant createdAt, Period period) {}

	private final InsightsProperties props;

	public List<SignalType> forProduct(ProductInsightResponse r, Instant createdAt, Period period) {
		List<SignalType> out = new ArrayList<>();
		long wanted = wanted(r);
		if (!r.preorder() && r.stock() > 0 && r.daysLeft() != null
				&& r.daysLeft().compareTo(BigDecimal.valueOf(props.restockDays())) <= 0) out.add(SignalType.RESTOCK_SOON);
		if (!r.preorder() && r.stock() == 0 && wanted >= props.wantedSoldOutMin()) out.add(SignalType.WANTED_SOLD_OUT);
		if (r.stock() > 0 && wanted >= r.stock()) out.add(SignalType.LOW_STOCK_HIGH_DEMAND);
		if (r.views() >= props.lookedViewsMin() && r.conversion().compareTo(props.lookedConversionMax()) < 0)
			out.add(SignalType.LOOKED_NOT_BOUGHT);
		if (!r.preorder() && r.stock() > 0 && r.units() == 0 && createdAt.isBefore(period.start())) out.add(SignalType.SLOW_MOVER);
		return out;
	}

	/** Every signal, in {@link SignalType} order, each type ranked by its own number; at most {@code signalLimit}. */
	public List<SignalResponse> build(List<Candidate> products, List<SearchTermCount> zeroResultTerms,
									  List<EarlyAccessCount> earlyAccess) {
		List<Ranked> all = new ArrayList<>();
		for (Candidate c : products) {
			for (SignalType type : forProduct(c.row(), c.createdAt(), c.period())) all.add(forProduct(type, c.row()));
		}
		for (SearchTermCount t : zeroResultTerms) {
			if (t.zeroResults() >= props.searchGapMin()) {
				all.add(new Ranked(new SignalResponse(SignalType.SEARCH_GAP, "Searched, not found",
						"\"%s\" was searched %d times and found nothing.".formatted(t.term(), t.zeroResults()),
						null, null, t.term(), null, t.zeroResults()), -t.zeroResults()));
			}
		}
		for (EarlyAccessCount e : earlyAccess) {
			if (e.signups() >= 1) {
				all.add(new Ranked(new SignalResponse(SignalType.COMING_SOON_DEMAND, "Coming-soon demand",
						"%s: %d early-access %s.".formatted(e.categoryName(), e.signups(), e.signups() == 1 ? "signup" : "signups"),
						null, null, null, e.categorySlug(), e.signups()), -e.signups()));
			}
		}
		return all.stream()
				.sorted(Comparator.comparing((Ranked r) -> r.signal().type()).thenComparingDouble(Ranked::rank))
				.limit(props.signalLimit())
				.map(Ranked::signal)
				.toList();
	}

	/** {@code rank}: lower comes first. */
	private record Ranked(SignalResponse signal, double rank) {}

	private static Ranked forProduct(SignalType type, ProductInsightResponse r) {
		long wanted = wanted(r);
		return switch (type) {
			case RESTOCK_SOON -> {
				long days = r.daysLeft().setScale(0, RoundingMode.CEILING).longValue();
				yield ranked(type, "Restock soon", "%s: about %s days of stock left (%d left, %d sold)."
						.formatted(r.name(), r.daysLeft().toPlainString(), r.stock(), r.units()), r, days, r.daysLeft().doubleValue());
			}
			case WANTED_SOLD_OUT -> ranked(type, "Wanted but sold out",
					"%s is sold out and %d shoppers have it saved or in their cart.".formatted(r.name(), wanted), r, wanted, -wanted);
			case LOW_STOCK_HIGH_DEMAND -> ranked(type, "More demand than stock",
					"%s: %d shoppers want it and %d are left.".formatted(r.name(), wanted, r.stock()), r, wanted,
					-(double) wanted / r.stock());
			case LOOKED_NOT_BOUGHT -> ranked(type, "Looked at, not bought",
					"%s: %d views but %d orders. Check the price, photos or description.".formatted(r.name(), r.views(), r.orders()),
					r, r.views(), -r.views());
			case SLOW_MOVER -> ranked(type, "Not selling",
					"%s: %d in stock and none sold in this period.".formatted(r.name(), r.stock()), r, r.stock(), -r.stock());
			default -> throw new IllegalArgumentException(type.name());
		};
	}

	private static Ranked ranked(SignalType type, String title, String reason, ProductInsightResponse r, long value, double rank) {
		return new Ranked(new SignalResponse(type, title, reason, r.id(), r.slug(), null, null, value), rank);
	}

	private static long wanted(ProductInsightResponse r) {
		return r.wishlists() + r.cartMembers();
	}
}
