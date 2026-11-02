package com.ecommerce.review.service;

import com.ecommerce.catalog.api.CatalogApi;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.order.api.PurchaseVerifier;
import com.ecommerce.review.dto.request.ReviewRequest;
import com.ecommerce.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewServiceTest {

	private final ReviewRepository reviews = mock(ReviewRepository.class);
	private final CatalogApi catalog = mock(CatalogApi.class);
	private final PurchaseVerifier verifier = mock(PurchaseVerifier.class);
	private final ReviewService service = new ReviewService(reviews, catalog, verifier, mock(CurrentUser.class));

	@Test
	void onlyBuyersWhoReceivedTheProductMayReview() {
		when(catalog.activeProductIdBySlug("kitsune-set")).thenReturn(Optional.of(4L));
		when(verifier.hasReceived(9L, 4L)).thenReturn(false);
		ReviewRequest r = new ReviewRequest();
		r.setRating(5);
		r.setBody("Great costume, fits well.");
		assertThatThrownBy(() -> service.create("kitsune-set", 9L, "Tasnim R.", r))
				.extracting(e -> ((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
		verify(reviews, never()).save(any());
	}

	@Test
	void displayNameNeverShowsAFullSurname() {
		assertThat(ReviewService.displayName("Tasnim Rahman")).isEqualTo("Tasnim R.");
		assertThat(ReviewService.displayName("Arif")).isEqualTo("Arif");
		assertThat(ReviewService.displayName("  ")).isEqualTo("Verified buyer");
		assertThat(ReviewService.displayName(null)).isEqualTo("Verified buyer");
	}
}
