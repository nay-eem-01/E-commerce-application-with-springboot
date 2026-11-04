package com.ecommerce.catalog.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Read-only numbers the insights dashboard needs from the catalog. Days are shop days, both ends inclusive. */
public interface CatalogReport {

	record ProductSnapshot(Long id, String slug, String name, String category, String character,
						   boolean active, boolean preorder, int stock, Instant createdAt) {}

	record TrafficTotals(long views, long searches, long zeroResultSearches) {}

	record SearchTermCount(String term, long searches, long zeroResults) {}

	record EarlyAccessCount(String categorySlug, String categoryName, long signups) {}

	/** Every product, any status; {@code character} is null when it has none. */
	List<ProductSnapshot> products();

	Map<Long, Long> views(LocalDate from, LocalDate to);

	TrafficTotals totals(LocalDate from, LocalDate to);

	List<SearchTermCount> topSearches(LocalDate from, LocalDate to, int limit);

	/** Terms that found nothing at least once, most often first. */
	List<SearchTermCount> topZeroResultSearches(LocalDate from, LocalDate to, int limit);

	/** Active coming-soon categories with their signup counts. */
	List<EarlyAccessCount> earlyAccess();
}
