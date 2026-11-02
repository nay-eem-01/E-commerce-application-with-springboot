package com.ecommerce.common.web;

import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.exception.ResponseException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagesTest {

	private static final Set<String> SORTS = Set.of("creationDate", "price");

	@Test
	void clampsPageSizeTo100AndNegativePageToZero() {
		Pageable p = Pages.of(-3, 5000, "price", AscOrDescType.asc, SORTS);
		assertThat(p.getPageSize()).isEqualTo(100);
		assertThat(p.getPageNumber()).isZero();
		assertThat(p.getSort().getOrderFor("price").getDirection()).isEqualTo(Sort.Direction.ASC);
	}

	@Test
	void unknownSortFieldIsA400NotAHibernate500() {
		assertThatThrownBy(() -> Pages.of(0, 20, "passwordHash", AscOrDescType.asc, SORTS))
				.isInstanceOf(ResponseException.class)
				.extracting(e -> ((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void blankSortFallsBackToCreationDateWithIdTieBreaker() {
		Pageable p = Pages.of(0, 20, "", AscOrDescType.desc, SORTS);
		assertThat(p.getSort().getOrderFor("creationDate")).isNotNull();
		assertThat(p.getSort().getOrderFor("id")).isNotNull();
	}
}
