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

	@Test
	void adminListFiltersByStatusAndNamesArchivedProducts() {
		com.ecommerce.review.entity.Review r = new com.ecommerce.review.entity.Review();
		org.springframework.test.util.ReflectionTestUtils.setField(r, "id", 31L);
		org.springframework.test.util.ReflectionTestUtils.setField(r, "creationDate", java.time.Instant.parse("2026-11-01T10:00:00Z"));
		r.setProductId(4L);
		r.setUserId(9L);
		r.setRating(2);
		r.setBody("Arrived scratched.");
		r.setAuthorName("Tasnim R.");
		r.setStatus(com.ecommerce.review.enums.ReviewStatus.PUBLISHED);
		when(reviews.adminSearch(org.mockito.ArgumentMatchers.eq(com.ecommerce.review.enums.ReviewStatus.PUBLISHED),
				org.mockito.ArgumentMatchers.isNull(), any()))
				.thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(r)));
		// the product was archived since; the review list still names it
		when(catalog.names(java.util.Set.of(4L))).thenReturn(java.util.Map.of(4L, "Kitsune school-run set"));

		var page = service.adminList(com.ecommerce.review.enums.ReviewStatus.PUBLISHED, null, 0, 20);

		assertThat(page.content()).singleElement().satisfies(a -> {
			assertThat(a.id()).isEqualTo(31L);
			assertThat(a.productName()).isEqualTo("Kitsune school-run set");
			assertThat(a.rating()).isEqualTo(2);
			assertThat(a.createdAt()).isEqualTo(java.time.Instant.parse("2026-11-01T10:00:00Z"));
		});
		// staff see who wrote it by display name only: no user id or contact details in the response
		assertThat(com.ecommerce.review.dto.response.AdminReviewResponse.class.getRecordComponents())
				.extracting(java.lang.reflect.RecordComponent::getName).doesNotContain("userId", "phone", "email");
	}
}
