package com.ecommerce.catalog.service;

import com.ecommerce.catalog.repository.TrafficRepository;
import com.ecommerce.catalog.repository.TrafficRepository.SearchRow;
import com.ecommerce.catalog.repository.TrafficRepository.ViewRow;
import com.ecommerce.common.time.ShopTime;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Pattern;

/**
 * Counts product page views and searches in memory and adds them to the daily tables once a minute,
 * so a shopper's request never waits on a write. Counts from several instances add up in the same rows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogTraffic {

	static final int TERM_MAX = 60;
	static final int KEEP_DAYS = 400;
	private static final Pattern BOT = Pattern.compile("bot|crawl|spider|slurp|preview|facebookexternalhit|headless",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern SEVEN_DIGITS = Pattern.compile("(\\D*\\d){7}");
	private static final Pattern SPACES = Pattern.compile("\\s+");

	private record ViewKey(LocalDate day, long productId) {}

	private record TermKey(LocalDate day, String term) {}

	private record TermCount(LongAdder searches, LongAdder zeroResults) {
		TermCount() { this(new LongAdder(), new LongAdder()); }
	}

	private final TrafficRepository repository;
	private final Clock clock;

	// shortcut: an increment racing a flush can land in the map just swapped out and be lost; fine for approximate counts.
	private final AtomicReference<Map<ViewKey, LongAdder>> views = new AtomicReference<>(new ConcurrentHashMap<>());
	private final AtomicReference<Map<TermKey, TermCount>> searches = new AtomicReference<>(new ConcurrentHashMap<>());

	public void productViewed(long productId, String userAgent) {
		if (isBot(userAgent)) return;
		views.get().computeIfAbsent(new ViewKey(today(), productId), k -> new LongAdder()).increment();
	}

	/** Counts the first page of a text search only; later pages are the same search. */
	public void searched(String q, int pageNo, long total, String userAgent) {
		if (pageNo != 0 || isBot(userAgent)) return;
		normaliseTerm(q).ifPresent(term -> {
			TermCount c = searches.get().computeIfAbsent(new TermKey(today(), term), k -> new TermCount());
			c.searches().increment();
			if (total == 0) c.zeroResults().increment();
		});
	}

	@PreDestroy
	@Scheduled(fixedDelayString = "${app.insights.flush-ms:60000}", initialDelayString = "${app.insights.flush-ms:60000}")
	public void flush() {
		Map<ViewKey, LongAdder> v = views.getAndSet(new ConcurrentHashMap<>());
		if (!v.isEmpty()) {
			List<ViewRow> rows = new ArrayList<>();
			v.forEach((k, n) -> rows.add(new ViewRow(k.day(), k.productId(), n.sum())));
			try {
				repository.addViews(rows);
			} catch (RuntimeException e) {
				log.warn("Product view counts not saved, will retry: {}", e.toString());
				v.forEach((k, n) -> views.get().computeIfAbsent(k, x -> new LongAdder()).add(n.sum()));
			}
		}
		Map<TermKey, TermCount> s = searches.getAndSet(new ConcurrentHashMap<>());
		if (!s.isEmpty()) {
			List<SearchRow> rows = new ArrayList<>();
			s.forEach((k, c) -> rows.add(new SearchRow(k.day(), k.term(), c.searches().sum(), c.zeroResults().sum())));
			try {
				repository.addSearches(rows);
			} catch (RuntimeException e) {
				log.warn("Search counts not saved, will retry: {}", e.toString());
				s.forEach((k, c) -> {
					TermCount back = searches.get().computeIfAbsent(k, x -> new TermCount());
					back.searches().add(c.searches().sum());
					back.zeroResults().add(c.zeroResults().sum());
				});
			}
		}
	}

	@Scheduled(cron = "0 30 3 * * *", zone = "Asia/Dhaka")
	public void purgeOld() {
		repository.deleteBefore(today().minusDays(KEEP_DAYS));
	}

	static boolean isBot(String userAgent) {
		return userAgent == null || userAgent.isBlank() || BOT.matcher(userAgent).find();
	}

	/** Lower case, single spaces, at most 60 characters; empty for blank terms and ones that may hold a phone or order number. */
	static Optional<String> normaliseTerm(String q) {
		if (q == null) return Optional.empty();
		String term = SPACES.matcher(q.trim()).replaceAll(" ").toLowerCase(Locale.ROOT);
		if (term.isEmpty() || SEVEN_DIGITS.matcher(term).lookingAt()) return Optional.empty();
		return Optional.of(term.length() > TERM_MAX ? term.substring(0, TERM_MAX).trim() : term);
	}

	private LocalDate today() {
		return LocalDate.now(clock.withZone(ShopTime.ZONE));
	}
}
