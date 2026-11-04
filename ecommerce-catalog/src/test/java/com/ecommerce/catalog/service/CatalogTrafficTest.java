package com.ecommerce.catalog.service;

import com.ecommerce.catalog.repository.TrafficRepository;
import com.ecommerce.catalog.repository.TrafficRepository.SearchRow;
import com.ecommerce.catalog.repository.TrafficRepository.ViewRow;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CatalogTrafficTest {

	private static final String CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36";
	// 20:00 UTC on 4 Nov is 02:00 on 5 Nov in Dhaka
	private final Clock clock = Clock.fixed(Instant.parse("2026-11-04T20:00:00Z"), ZoneOffset.UTC);
	private final TrafficRepository repository = mock(TrafficRepository.class);
	private final CatalogTraffic traffic = new CatalogTraffic(repository, clock);

	@SuppressWarnings("unchecked")
	private <T> List<T> captured(ArgumentCaptor<List> captor) {
		return (List<T>) captor.getValue();
	}

	@Test
	void botsAndMissingUserAgentAreNotCounted() {
		assertThat(CatalogTraffic.isBot(null)).isTrue();
		assertThat(CatalogTraffic.isBot(" ")).isTrue();
		assertThat(CatalogTraffic.isBot("Mozilla/5.0 (compatible; Googlebot/2.1)")).isTrue();
		assertThat(CatalogTraffic.isBot("facebookexternalhit/1.1")).isTrue();
		assertThat(CatalogTraffic.isBot("Mozilla/5.0 HeadlessChrome/130.0")).isTrue();
		assertThat(CatalogTraffic.isBot(CHROME)).isFalse();

		traffic.productViewed(7L, "Googlebot");
		traffic.flush();
		verify(repository, never()).addViews(anyList());
	}

	@Test
	void termsAreNormalised() {
		assertThat(CatalogTraffic.normaliseTerm("  Kitsune   BOTTLE ")).contains("kitsune bottle");
		assertThat(CatalogTraffic.normaliseTerm("a".repeat(90)).orElseThrow()).hasSize(60);
		assertThat(CatalogTraffic.normaliseTerm("call 01712345678")).isEmpty();
		assertThat(CatalogTraffic.normaliseTerm("   ")).isEmpty();
		assertThat(CatalogTraffic.normaliseTerm(null)).isEmpty();
	}

	@Test
	void onlyFirstPageNonBlankSearchesCount() {
		traffic.searched("ramen", 1, 5, CHROME);
		traffic.searched("", 0, 0, CHROME);
		traffic.searched(null, 0, 0, CHROME);
		traffic.flush();
		verify(repository, never()).addSearches(anyList());
	}

	@Test
	void countsAreSummedPerDayAndFlushedOnce() {
		traffic.productViewed(7L, CHROME);
		traffic.productViewed(7L, CHROME);
		traffic.productViewed(7L, CHROME);
		traffic.productViewed(9L, CHROME);
		traffic.searched("Ramen", 0, 0, CHROME);
		traffic.searched("ramen ", 0, 0, CHROME);
		traffic.searched("kitsune", 0, 4, CHROME);

		traffic.flush();

		LocalDate day = LocalDate.of(2026, 11, 5);
		ArgumentCaptor<List> views = ArgumentCaptor.forClass(List.class);
		verify(repository).addViews(views.capture());
		assertThat(this.<ViewRow>captured(views)).containsExactlyInAnyOrder(new ViewRow(day, 7L, 3), new ViewRow(day, 9L, 1));
		ArgumentCaptor<List> searches = ArgumentCaptor.forClass(List.class);
		verify(repository).addSearches(searches.capture());
		assertThat(this.<SearchRow>captured(searches))
				.containsExactlyInAnyOrder(new SearchRow(day, "ramen", 2, 2), new SearchRow(day, "kitsune", 1, 0));

		traffic.flush();
		verify(repository, times(1)).addViews(anyList());
		verify(repository, times(1)).addSearches(anyList());
	}

	@Test
	void failedFlushKeepsCounts() {
		traffic.productViewed(7L, CHROME);
		traffic.productViewed(7L, CHROME);
		doThrow(new RuntimeException("database down")).doNothing().when(repository).addViews(anyList());

		traffic.flush();
		traffic.productViewed(7L, CHROME);
		traffic.flush();

		ArgumentCaptor<List> views = ArgumentCaptor.forClass(List.class);
		verify(repository, times(2)).addViews(views.capture());
		@SuppressWarnings("unchecked")
		List<ViewRow> second = (List<ViewRow>) views.getAllValues().get(1);
		assertThat(second).containsExactly(new ViewRow(LocalDate.of(2026, 11, 5), 7L, 3));
	}

	@Test
	void newTermsStopAtTheDailyCapButKnownTermsStillCount() {
		traffic.searched("ramen", 0, 1, CHROME);
		for (int i = 1; i < CatalogTraffic.MAX_TERMS_PER_DAY + 50; i++) {
			traffic.searched("term-" + Integer.toString(i, 36), 0, 1, CHROME);
			if (i % 5_000 == 0) traffic.flush(); // the cap is per day, not per flush window
		}
		traffic.searched("ramen", 0, 1, CHROME);
		traffic.flush();

		ArgumentCaptor<List> searches = ArgumentCaptor.forClass(List.class);
		verify(repository, times(5)).addSearches(searches.capture());
		@SuppressWarnings("unchecked")
		long rows = searches.getAllValues().stream().mapToLong(l -> ((List<SearchRow>) l).size()).sum();
		// the cap admits MAX_TERMS_PER_DAY distinct terms; "ramen" is in the first and the last window, so one extra row
		assertThat(rows).isEqualTo(CatalogTraffic.MAX_TERMS_PER_DAY + 1);
		assertThat(this.<SearchRow>captured(searches)).contains(new SearchRow(LocalDate.of(2026, 11, 5), "ramen", 1, 0));
	}

	@Test
	void emailLikeTermsAreDropped() {
		assertThat(CatalogTraffic.normaliseTerm("rahim@example.com")).isEmpty();
	}
}
