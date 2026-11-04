package com.ecommerce.catalog.service;

import com.ecommerce.catalog.dto.request.ProductRequest;
import com.ecommerce.catalog.dto.response.ProductEditResponse;
import com.ecommerce.catalog.entity.Category;
import com.ecommerce.catalog.entity.Product;
import com.ecommerce.catalog.entity.ProductMedia;
import com.ecommerce.catalog.entity.ProductOption;
import com.ecommerce.catalog.entity.ProductTag;
import com.ecommerce.catalog.entity.ShopCharacter;
import com.ecommerce.catalog.enums.MediaKind;
import com.ecommerce.catalog.enums.ProductStatus;
import com.ecommerce.catalog.media.ImageStorage;
import com.ecommerce.catalog.repository.ProductRepository;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductAdminServiceTest {

	private static final String UPLOADED = "https://cdn.senpai.shop/3f2b8c1e-5a7d-4e2f-9b1c-0d6e8a4f2b7c.webp";
	private static final String FOREIGN = "https://old-host.example/bottle.jpg";

	private final ProductRepository products = mock(ProductRepository.class);
	private final CategoryService categories = mock(CategoryService.class);
	private final CharacterService characters = mock(CharacterService.class);
	private final ImageStorage storage = mock(ImageStorage.class);
	private final ProductAdminService service = new ProductAdminService(products, categories, characters, storage);

	private final Category bottles = category();
	private final ShopCharacter kitsune = character();

	{
		when(categories.find(3L)).thenReturn(bottles);
		when(characters.find(4L)).thenReturn(kitsune);
		when(storage.isStored(UPLOADED)).thenReturn(true);
		when(products.save(any())).thenAnswer(i -> i.getArgument(0));
		when(products.existsBySlug(anyString())).thenReturn(false);
		when(products.existsBySlugAndIdNot(anyString(), anyLong())).thenReturn(false);
	}

	private static Category category() {
		Category c = new Category();
		ReflectionTestUtils.setField(c, "id", 3L);
		c.setSlug("bottles");
		c.setName("Bottles");
		return c;
	}

	private static ShopCharacter character() {
		ShopCharacter c = new ShopCharacter();
		ReflectionTestUtils.setField(c, "id", 4L);
		c.setSlug("kitsune");
		c.setName("Kitsune");
		return c;
	}

	/** A saved product: two colour options (stock 4 and 0), a photo from another host, two spin frames, a tag. */
	private Product saved() {
		Product p = new Product();
		ReflectionTestUtils.setField(p, "id", 7L);
		p.setSlug("kitsune-bottle");
		p.setName("Kitsune bottle");
		p.setSummary("A bottle.");
		p.setCategory(bottles);
		p.setCharacter(kitsune);
		p.setPrice(new BigDecimal("1450.00"));
		p.setStatus(ProductStatus.ACTIVE);
		p.setOptionName("Colour");
		p.getOptions().add(option(p, 1L, "Petal pink", 4, 0));
		p.getOptions().add(option(p, 2L, "Night navy", 0, 1));
		p.recomputeStock();
		p.getMedia().add(media(p, MediaKind.PHOTO, FOREIGN, 0));
		p.getMedia().add(media(p, MediaKind.SPIN_FRAME, UPLOADED, 1));
		p.getMedia().add(media(p, MediaKind.SPIN_FRAME, UPLOADED.replace("3f2b", "4f2b"), 2));
		ProductTag tag = new ProductTag();
		tag.setProduct(p);
		tag.setGroup("Capacity");
		tag.setValue("750 ml");
		p.getTags().add(tag);
		when(products.findById(7L)).thenReturn(Optional.of(p));
		return p;
	}

	private static ProductOption option(Product p, Long id, String value, int stock, int order) {
		ProductOption o = new ProductOption();
		ReflectionTestUtils.setField(o, "id", id);
		o.setProduct(p);
		o.setValue(value);
		o.setPriceDelta(new BigDecimal("0.00")); // as read back from NUMERIC(12,2)
		o.setStock(stock);
		o.setSortOrder(order);
		return o;
	}

	private static ProductMedia media(Product p, MediaKind kind, String url, int order) {
		ProductMedia m = new ProductMedia();
		m.setProduct(p);
		m.setKind(kind);
		m.setUrl(url);
		m.setSortOrder(order);
		return m;
	}

	/** What the editor sends back: the edit view turned into a request, unchanged. */
	private static ProductRequest toRequest(ProductEditResponse e) {
		ProductRequest r = new ProductRequest();
		r.setSlug(e.slug());
		r.setCategoryId(e.categoryId());
		r.setCharacterId(e.characterId());
		r.setName(e.name());
		r.setSummary(e.summary());
		r.setPrice(e.price());
		r.setStock(e.stock());
		r.setStatus(e.status());
		r.setOptionName(e.optionName());
		r.setOptions(e.options().stream().map(o -> {
			ProductRequest.OptionRequest or = new ProductRequest.OptionRequest();
			or.setValue(o.value());
			or.setPriceDelta(o.priceDelta());
			or.setStock(o.stock());
			or.setSwatch(o.swatch());
			return or;
		}).toList());
		r.setMedia(e.media().stream().map(m -> {
			ProductRequest.MediaRequest mr = new ProductRequest.MediaRequest();
			mr.setKind(m.kind());
			mr.setUrl(m.url());
			mr.setAltText(m.altText());
			return mr;
		}).toList());
		r.setTags(e.tags().stream().map(t -> {
			ProductRequest.TagRequest tr = new ProductRequest.TagRequest();
			tr.setGroup(t.group());
			tr.setValue(t.value());
			return tr;
		}).toList());
		r.setBoxItems(e.boxItems());
		r.setBundleProductIds(e.bundleProductIds());
		return r;
	}

	private static ProductRequest.MediaRequest photo(String url) {
		ProductRequest.MediaRequest m = new ProductRequest.MediaRequest();
		m.setKind(MediaKind.PHOTO);
		m.setUrl(url);
		return m;
	}

	@Test
	void editViewHasIdsAndExactStock() {
		ProductEditResponse e = service.editView(saved().getId());
		assertThat(e.categoryId()).isEqualTo(3L);
		assertThat(e.characterId()).isEqualTo(4L);
		assertThat(e.options()).extracting(ProductEditResponse.Option::stock).containsExactly(4, 0);
		assertThat(e.media()).extracting(ProductEditResponse.Media::kind)
				.containsExactly(MediaKind.PHOTO, MediaKind.SPIN_FRAME, MediaKind.SPIN_FRAME);
	}

	@Test
	void editViewRoundTripsWithoutChangingStock() {
		Product p = saved();
		ProductEditResponse before = service.editView(7L);
		service.update(7L, toRequest(before));
		assertThat(p.getOptions()).extracting(ProductOption::getStock).containsExactly(4, 0);
		assertThat(p.getOptions()).extracting(ProductOption::getId).containsExactly(1L, 2L);
		assertThat(service.editView(7L)).isEqualTo(before);
	}

	@Test
	void keepsExistingForeignUrlButRejectsItElsewhere() {
		saved();
		ProductRequest edit = toRequest(service.editView(7L));
		service.update(7L, edit); // the old foreign photo stays

		ProductRequest other = toRequest(service.editView(7L));
		other.setSlug("another-bottle");
		other.setMedia(List.of(photo(FOREIGN)));
		assertThatThrownBy(() -> service.create(other))
				.isInstanceOf(ResponseException.class).hasMessage(ErrorCode.IMAGE_NOT_UPLOADED);
	}

	@Test
	void newForeignUrlOnAnExistingProductIs400() {
		saved();
		ProductRequest edit = toRequest(service.editView(7L));
		edit.setMedia(List.of(photo("https://evil.example/x.webp")));
		assertThatThrownBy(() -> service.update(7L, edit))
				.isInstanceOf(ResponseException.class).hasMessage(ErrorCode.IMAGE_NOT_UPLOADED);
	}

	@Test
	void acceptsUploadedUrlOnCreate() {
		saved();
		ProductRequest create = toRequest(service.editView(7L));
		create.setSlug("new-bottle");
		create.setMedia(List.of(photo(UPLOADED)));
		assertThat(service.create(create).photos()).hasSize(1);
	}

	/**
	 * Regression: saving a set or a tagged product unchanged failed with 409. Clearing and re-adding the same tag or set
	 * item makes Hibernate insert the new row before deleting the old one, which trips the unique constraints
	 * (product_tags_unique, product_bundle_items_unique). The same rows must be kept instead.
	 */
	@Test
	void savingTheSameTagsAndSetItemsKeepsTheRows() {
		Product p = saved();
		Product part = new Product();
		ReflectionTestUtils.setField(part, "id", 9L);
		part.setSlug("ramen-bag");
		part.setName("Ramen bag");
		part.setCategory(bottles);
		part.setPrice(new BigDecimal("900.00"));
		part.setStatus(ProductStatus.ACTIVE);
		when(products.findById(9L)).thenReturn(Optional.of(part));
		com.ecommerce.catalog.entity.ProductBundleItem item = new com.ecommerce.catalog.entity.ProductBundleItem();
		item.setBundle(p);
		item.setComponent(part);
		p.getBundleItems().add(item);
		ProductTag tag = p.getTags().getFirst();

		ProductRequest same = toRequest(service.editView(7L));
		ProductRequest.TagRequest extra = new ProductRequest.TagRequest();
		extra.setGroup("Material");
		extra.setValue("Steel");
		same.setTags(new java.util.ArrayList<>(same.getTags()));
		same.getTags().add(0, extra); // a new tag in front of the kept one
		service.update(7L, same);

		assertThat(p.getTags()).hasSize(2).anySatisfy(t -> assertThat(t).isSameAs(tag));
		assertThat(tag.getSortOrder()).isEqualTo(1);
		assertThat(p.getBundleItems()).singleElement().isSameAs(item);
	}

	@Test
	void slugTakenNamesTheSlugField() {
		saved();
		when(products.existsBySlug("kitsune-bottle")).thenReturn(true);
		ProductRequest copy = toRequest(service.editView(7L));
		assertThatThrownBy(() -> service.create(copy)).isInstanceOfSatisfying(ResponseException.class, e -> {
			assertThat(e.getMessage()).isEqualTo(ErrorCode.PRODUCT_SLUG_TAKEN);
			assertThat(e.getPayload()).isEqualTo(java.util.Map.of("field", "slug"));
		});
	}
}
