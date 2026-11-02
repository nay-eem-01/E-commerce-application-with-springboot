package com.ecommerce.catalog.service;

import com.ecommerce.catalog.api.LineRequest;
import com.ecommerce.catalog.entity.Category;
import com.ecommerce.catalog.entity.Product;
import com.ecommerce.catalog.entity.ProductOption;
import com.ecommerce.catalog.enums.ProductStatus;
import com.ecommerce.catalog.repository.ProductOptionRepository;
import com.ecommerce.catalog.repository.ProductRepository;
import com.ecommerce.common.exception.ResponseException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogApiImplTest {

	private final ProductRepository products = mock(ProductRepository.class);
	private final ProductOptionRepository options = mock(ProductOptionRepository.class);
	private final CatalogApiImpl api = new CatalogApiImpl(products, options);

	private Product product(Long id, String price, boolean withOptions) {
		Product p = new Product();
		p.setId(id);
		p.setSlug("p" + id);
		p.setName("P" + id);
		p.setPrice(new BigDecimal(price));
		p.setStatus(ProductStatus.ACTIVE);
		p.setCategory(new Category());
		p.setStock(4);
		if (withOptions) {
			p.setOptionName("Edition");
			ProductOption o = new ProductOption();
			o.setId(10L);
			o.setProduct(p);
			o.setValue("Deluxe");
			o.setPriceDelta(new BigDecimal("1200"));
			o.setStock(1);
			p.getOptions().add(o);
		}
		return p;
	}

	@Test
	void optionPriceIsProductPricePlusDelta() {
		when(products.findByIdIn(anyList())).thenReturn(List.of(product(1L, "6800", true)));
		var line = api.price(List.of(new LineRequest(1L, 10L, 1))).getFirst();
		assertThat(line.unitPrice()).isEqualByComparingTo("8000.00");
		assertThat(line.availableStock()).isEqualTo(1);
		assertThat(line.optionValue()).isEqualTo("Deluxe");
	}

	@Test
	void optionRulesAndInactiveProducts() {
		when(products.findByIdIn(anyList())).thenReturn(List.of(product(1L, "6800", true)));
		assertThatThrownBy(() -> api.price(List.of(new LineRequest(1L, null, 1)))).isInstanceOf(ResponseException.class);
		assertThatThrownBy(() -> api.price(List.of(new LineRequest(1L, 99L, 1)))).isInstanceOf(ResponseException.class);

		Product archived = product(2L, "100", false);
		archived.setStatus(ProductStatus.ARCHIVED);
		when(products.findByIdIn(anyList())).thenReturn(List.of(archived));
		assertThatThrownBy(() -> api.price(List.of(new LineRequest(2L, null, 1))))
				.extracting(e -> ((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void reserveFailsWholeOrderWith409NamingShortProducts() {
		when(products.takeStock(1L, 1)).thenReturn(1);
		when(products.takeStock(2L, 3)).thenReturn(0);
		assertThatThrownBy(() -> api.reserve(List.of(new LineRequest(2L, null, 3), new LineRequest(1L, null, 1))))
				.isInstanceOf(ResponseException.class)
				.satisfies(e -> {
					ResponseException re = (ResponseException) e;
					assertThat(re.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(re.getPayload().toString()).contains("2");
				});
	}

	@Test
	void reserveLocksRowsInProductIdOrderToAvoidDeadlocks() {
		when(products.takeStock(1L, 1)).thenReturn(1);
		when(products.takeStock(2L, 1)).thenReturn(1);
		api.reserve(List.of(new LineRequest(2L, null, 1), new LineRequest(1L, null, 1)));
		var order = inOrder(products);
		order.verify(products).takeStock(1L, 1);
		order.verify(products).takeStock(2L, 1);
	}

	/**
	 * Regression: cart merge/view catch "unavailable" to skip a line. price() must not mark the
	 * caller's transaction rollback-only for that, or the caller's commit fails with a 500.
	 */
	@Test
	void priceDoesNotRollBackTheCallerOnExpectedFailures() throws Exception {
		Transactional tx = CatalogApiImpl.class.getMethod("price", List.class).getAnnotation(Transactional.class);
		assertThat(Arrays.asList(tx.noRollbackFor())).contains(ResponseException.class);
		assertThat(tx.readOnly()).isTrue();
	}
}
