package com.ecommerce.catalog.service;

import com.ecommerce.catalog.dto.request.EarlyAccessRequest;
import com.ecommerce.catalog.entity.Category;
import com.ecommerce.catalog.entity.EarlyAccessSignup;
import com.ecommerce.catalog.repository.CategoryRepository;
import com.ecommerce.catalog.repository.EarlyAccessSignupRepository;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.ratelimit.InMemoryRateLimiter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EarlyAccessServiceTest {

	private final CategoryRepository categories = mock(CategoryRepository.class);
	private final EarlyAccessSignupRepository signups = mock(EarlyAccessSignupRepository.class);
	private final EarlyAccessService service = new EarlyAccessService(categories, signups, new InMemoryRateLimiter());

	private Category apparel(boolean comingSoon) {
		Category c = new Category();
		ReflectionTestUtils.setField(c, "id", 3L);
		c.setSlug("apparel");
		c.setName("Apparel");
		c.setComingSoon(comingSoon);
		return c;
	}

	private EarlyAccessRequest request(String phone, String email) {
		EarlyAccessRequest r = new EarlyAccessRequest();
		r.setCategorySlug("apparel");
		r.setPhone(phone);
		r.setEmail(email);
		return r;
	}

	@Test
	void savesTheCanonicalPhoneAndLowercaseEmail() {
		when(categories.findBySlug("apparel")).thenReturn(Optional.of(apparel(true)));
		service.signUp(request("01712-345678", "Nusrat@Example.com"), "1.1.1.1");
		ArgumentCaptor<EarlyAccessSignup> saved = ArgumentCaptor.forClass(EarlyAccessSignup.class);
		verify(signups).save(saved.capture());
		assertThat(saved.getValue().getPhone()).isEqualTo("+8801712345678");
		assertThat(saved.getValue().getEmail()).isEqualTo("nusrat@example.com");
	}

	@Test
	void signingUpTwiceAnswersTheSameAndSavesOnce() {
		when(categories.findBySlug("apparel")).thenReturn(Optional.of(apparel(true)));
		when(signups.existsByCategoryIdAndPhone(3L, "+8801712345678")).thenReturn(true);
		service.signUp(request("01712345678", null), "1.1.1.1");
		verify(signups, never()).save(any());
	}

	@Test
	void onlyComingSoonCategoriesTakeSignups() {
		when(categories.findBySlug("apparel")).thenReturn(Optional.of(apparel(false)));
		assertThatThrownBy(() -> service.signUp(request("01712345678", null), "1.1.1.1"))
				.isInstanceOfSatisfying(ResponseException.class, e -> assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void needsAValidContact() {
		when(categories.findBySlug("apparel")).thenReturn(Optional.of(apparel(true)));
		assertThatThrownBy(() -> service.signUp(request(" ", ""), "1.1.1.1")).isInstanceOf(ResponseException.class);
		assertThatThrownBy(() -> service.signUp(request("12345", null), "1.1.1.1")).isInstanceOf(ResponseException.class);
	}

	@Test
	void isRateLimitedPerAddress() {
		when(categories.findBySlug("apparel")).thenReturn(Optional.of(apparel(true)));
		for (int i = 0; i < 10; i++) {
			service.signUp(request("0171234567" + (i % 10), null), "9.9.9.9");
		}
		assertThatThrownBy(() -> service.signUp(request("01812345678", null), "9.9.9.9"))
				.isInstanceOfSatisfying(ResponseException.class, e -> assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
	}
}
