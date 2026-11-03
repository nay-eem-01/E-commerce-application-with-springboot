package com.ecommerce.catalog.dto;

import com.ecommerce.catalog.dto.response.ProductDetailResponse;
import com.ecommerce.catalog.dto.response.ProductSummaryResponse;
import com.ecommerce.catalog.entity.Category;
import com.ecommerce.catalog.entity.Product;
import com.ecommerce.catalog.entity.ProductOption;
import com.ecommerce.catalog.entity.ProductTag;
import com.ecommerce.catalog.entity.ShopCharacter;
import com.ecommerce.catalog.enums.StockState;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProductResponsesTest {

	private Product bottle(Instant created) {
		Category category = new Category();
		category.setSlug("bottles");
		category.setName("Bottles");
		ShopCharacter kitsune = new ShopCharacter();
		kitsune.setSlug("kitsune");
		kitsune.setName("Kitsune");
		kitsune.setLabel("狐");
		Product p = new Product();
		p.setSlug("kitsune-night-bottle");
		p.setName("Kitsune Night steel bottle, 750 ml");
		p.setSummary("A bottle.");
		p.setCategory(category);
		p.setCharacter(kitsune);
		p.setPrice(new BigDecimal("1450.00"));
		p.setOptionName("Colour");
		p.getOptions().add(option(p, 1L, "Petal pink", "#f2a7c3", 20));
		p.getOptions().add(option(p, 2L, "Night navy", "#1f2a4d", 0));
		p.recomputeStock();
		ProductTag tag = new ProductTag();
		tag.setProduct(p);
		tag.setGroup("Capacity");
		tag.setValue("750 ml");
		p.getTags().add(tag);
		ReflectionTestUtils.setField(p, "creationDate", created);
		return p;
	}

	private ProductOption option(Product p, Long id, String value, String swatch, int stock) {
		ProductOption o = new ProductOption();
		ReflectionTestUtils.setField(o, "id", id);
		o.setProduct(p);
		o.setValue(value);
		o.setSwatch(swatch);
		o.setStock(stock);
		return o;
	}

	@Test
	void aCardCarriesItsColourSwatchesCharacterAndNewFlag() {
		ProductSummaryResponse card = ProductSummaryResponse.from(bottle(Instant.now().minus(Duration.ofDays(2))));
		assertThat(card.characterSlug()).isEqualTo("kitsune");
		assertThat(card.newArrival()).isTrue();
		assertThat(card.swatches()).extracting(ProductSummaryResponse.Swatch::value).containsExactly("Petal pink", "Night navy");
		assertThat(card.swatches().get(1).stockState()).isEqualTo(StockState.SOLD_OUT);
	}

	@Test
	void productsOlderThanThirtyDaysAreNotNew() {
		assertThat(ProductSummaryResponse.from(bottle(Instant.now().minus(Duration.ofDays(31)))).newArrival()).isFalse();
	}

	@Test
	void textOptionsAreNotSwatches() {
		Product p = bottle(Instant.now());
		p.getOptions().forEach(o -> o.setSwatch(null));
		assertThat(ProductSummaryResponse.from(p).swatches()).isEmpty();
	}

	@Test
	void aSetListsWhatItHoldsAndOffersItsSaving() {
		Product bottle = bottle(Instant.now());
		Product set = bottle(Instant.now());
		set.setSlug("kitsune-school-run-set");
		set.setPrice(new BigDecimal("3990.00"));
		set.setCompareAtPrice(new BigDecimal("4400.00"));
		com.ecommerce.catalog.entity.ProductBundleItem item = new com.ecommerce.catalog.entity.ProductBundleItem();
		item.setBundle(set);
		item.setComponent(bottle);
		set.getBundleItems().add(item);

		assertThat(ProductSummaryResponse.from(set).set()).isTrue();
		assertThat(ProductSummaryResponse.from(bottle).set()).isFalse();
		assertThat(ProductDetailResponse.from(set).bundleItems()).extracting(ProductSummaryResponse::slug).containsExactly("kitsune-night-bottle");
		ProductDetailResponse page = ProductDetailResponse.from(bottle, java.util.List.of(set));
		assertThat(page.sets()).hasSize(1);
		assertThat(page.sets().get(0).saving()).isEqualByComparingTo("410.00");
	}

	@Test
	void theProductPageNamesTheCharacterAndTags() {
		ProductDetailResponse page = ProductDetailResponse.from(bottle(Instant.now()));
		assertThat(page.character().label()).isEqualTo("狐");
		assertThat(page.tags()).containsExactly(new ProductDetailResponse.TagResponse("Capacity", "750 ml"));
	}
}
