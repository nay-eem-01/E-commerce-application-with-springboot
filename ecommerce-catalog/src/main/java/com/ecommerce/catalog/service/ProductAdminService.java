package com.ecommerce.catalog.service;

import com.ecommerce.catalog.dto.request.ProductRequest;
import com.ecommerce.catalog.dto.request.StockUpdateRequest;
import com.ecommerce.catalog.dto.response.ProductDetailResponse;
import com.ecommerce.catalog.dto.response.ProductEditResponse;
import com.ecommerce.catalog.dto.response.StockResponse;
import com.ecommerce.catalog.entity.Product;
import com.ecommerce.catalog.entity.ProductBoxItem;
import com.ecommerce.catalog.entity.ProductFact;
import com.ecommerce.catalog.entity.ProductMedia;
import com.ecommerce.catalog.entity.ProductOption;
import com.ecommerce.catalog.enums.ProductStatus;
import com.ecommerce.catalog.media.ImageStorage;
import com.ecommerce.catalog.repository.ProductRepository;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.money.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

/**
 * Admin writes. A PUT replaces the product's child lists. Option ids are kept when the
 * value is unchanged so carts and order history that point at an option stay valid.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAdminService {

	private final ProductRepository productRepository;
	private final CategoryService categoryService;
	private final CharacterService characterService;
	private final ImageStorage imageStorage;

	@Transactional(readOnly = true)
	public ProductDetailResponse get(Long id) {
		return ProductDetailResponse.from(find(id));
	}

	/** The product in the shape the editor saves, so a load-and-save never loses stock or ids. */
	@Transactional(readOnly = true)
	public ProductEditResponse editView(Long id) {
		return ProductEditResponse.from(find(id));
	}

	@Transactional(readOnly = true)
	public StockResponse stock(Long id) {
		return StockResponse.from(find(id));
	}

	@Transactional
	public ProductDetailResponse create(ProductRequest request) {
		if (productRepository.existsBySlug(request.getSlug())) {
			throw ResponseException.conflict(ErrorCode.PRODUCT_SLUG_TAKEN);
		}
		Product product = new Product();
		apply(product, request);
		Product saved = productRepository.save(product);
		log.info("Product {} created", saved.getId());
		return ProductDetailResponse.from(saved);
	}

	@Transactional
	public ProductDetailResponse update(Long id, ProductRequest request) {
		Product product = find(id);
		if (productRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
			throw ResponseException.conflict(ErrorCode.PRODUCT_SLUG_TAKEN);
		}
		apply(product, request);
		return ProductDetailResponse.from(product);
	}

	@Transactional
	public ProductDetailResponse updateStock(Long id, StockUpdateRequest request) {
		Product product = find(id);
		if (request.getOptionId() == null) {
			if (product.hasOptions()) {
				throw ResponseException.badRequest(ErrorCode.OPTION_REQUIRED);
			}
			product.setStock(request.getStock());
		} else {
			ProductOption option = product.getOptions().stream()
					.filter(o -> o.getId().equals(request.getOptionId())).findFirst()
					.orElseThrow(() -> ResponseException.badRequest(ErrorCode.OPTION_NOT_FOUND));
			option.setStock(request.getStock());
			product.recomputeStock();
		}
		return ProductDetailResponse.from(product);
	}

	/** Archive, never hard-delete: order history keeps pointing at the product. */
	@Transactional
	public void archive(Long id) {
		find(id).setStatus(ProductStatus.ARCHIVED);
	}

	private Product find(Long id) {
		return productRepository.findById(id).orElseThrow(() -> ResponseException.notFound(ErrorCode.PRODUCT_NOT_FOUND));
	}

	private void apply(Product p, ProductRequest r) {
		if (r.isPreorder() && (r.getPreorderRelease() == null || r.getDeposit() == null)) {
			throw ResponseException.badRequest(ErrorCode.PREORDER_FIELDS_REQUIRED);
		}
		p.setSlug(r.getSlug());
		p.setCategory(categoryService.find(r.getCategoryId()));
		p.setCharacter(r.getCharacterId() == null ? null : characterService.find(r.getCharacterId()));
		p.setName(r.getName().trim());
		p.setMaker(trim(r.getMaker()));
		p.setSummary(r.getSummary().trim());
		p.setDescription(trim(r.getDescription()));
		p.setStory(trim(r.getStory()));
		p.setStoryQuote(trim(r.getStoryQuote()));
		p.setSpecLine(trim(r.getSpecLine()));
		p.setPrice(Money.of(r.getPrice()));
		p.setCompareAtPrice(r.getCompareAtPrice() == null ? null : Money.of(r.getCompareAtPrice()));
		p.setPreorder(r.isPreorder());
		p.setPreorderRelease(r.isPreorder() ? YearMonth.parse(r.getPreorderRelease()).atDay(1) : null);
		p.setDeposit(r.isPreorder() ? Money.of(r.getDeposit()) : null);
		p.setSized(r.isSized());
		p.setStatus(r.getStatus());
		p.setOptionName(r.getOptions().isEmpty() ? null : trim(r.getOptionName()));

		replaceOptions(p, r.getOptions());
		replaceFacts(p, r.getFacts());
		replaceBox(p, r.getBoxItems());
		replaceMedia(p, r.getMedia());
		replaceTags(p, r.getTags());
		replaceBundle(p, r.getBundleProductIds());

		if (p.hasOptions()) {
			p.recomputeStock();
		} else {
			p.setStock(r.getStock());
		}
	}

	private void replaceOptions(Product p, List<ProductRequest.OptionRequest> requests) {
		List<ProductOption> keep = new java.util.ArrayList<>();
		int order = 0;
		for (ProductRequest.OptionRequest req : requests) {
			ProductOption option = p.getOptions().stream()
					.filter(o -> o.getValue().equalsIgnoreCase(req.getValue().trim()) && !keep.contains(o))
					.findFirst().orElseGet(ProductOption::new);
			option.setProduct(p);
			option.setValue(req.getValue().trim());
			option.setPriceDelta(Money.of(req.getPriceDelta()));
			option.setStock(req.getStock());
			option.setSwatch(req.getSwatch());
			option.setSortOrder(order++);
			keep.add(option);
		}
		p.getOptions().retainAll(keep);
		keep.stream().filter(o -> !p.getOptions().contains(o)).forEach(p.getOptions()::add);
	}

	private void replaceFacts(Product p, List<ProductRequest.FactRequest> requests) {
		p.getFacts().clear();
		int order = 0;
		for (ProductRequest.FactRequest req : requests) {
			ProductFact fact = new ProductFact();
			fact.setProduct(p);
			fact.setLabel(req.getLabel().trim());
			fact.setValue(req.getValue().trim());
			fact.setSortOrder(order++);
			p.getFacts().add(fact);
		}
	}

	/**
	 * A set holds ordinary products only: no nesting, no itself. Rows for products already in the set are kept (only
	 * their position changes): clearing and re-adding them makes Hibernate insert before it deletes, which breaks
	 * product_bundle_items_unique.
	 */
	private void replaceBundle(Product p, List<Long> ids) {
		List<com.ecommerce.catalog.entity.ProductBundleItem> keep = new java.util.ArrayList<>();
		int order = 0;
		for (Long id : new java.util.LinkedHashSet<>(ids)) {
			Product component = productRepository.findById(id).orElseThrow(() -> ResponseException.badRequest(ErrorCode.BUNDLE_ITEM_INVALID));
			if (component == p || (p.getId() != null && p.getId().equals(component.getId())) || component.isSet()) {
				throw ResponseException.badRequest(ErrorCode.BUNDLE_ITEM_INVALID);
			}
			com.ecommerce.catalog.entity.ProductBundleItem item = p.getBundleItems().stream()
					.filter(b -> b.getComponent() == component || (component.getId() != null && component.getId().equals(b.getComponent().getId())))
					.findFirst().orElseGet(() -> {
						com.ecommerce.catalog.entity.ProductBundleItem created = new com.ecommerce.catalog.entity.ProductBundleItem();
						created.setBundle(p);
						created.setComponent(component);
						return created;
					});
			item.setSortOrder(order++);
			keep.add(item);
		}
		p.getBundleItems().retainAll(keep);
		keep.stream().filter(b -> !p.getBundleItems().contains(b)).forEach(p.getBundleItems()::add);
	}

	/** Same group and value (any case) keeps its row, as for the set above: product_tags_unique. */
	private void replaceTags(Product p, List<ProductRequest.TagRequest> requests) {
		List<com.ecommerce.catalog.entity.ProductTag> keep = new java.util.ArrayList<>();
		int order = 0;
		java.util.Set<String> seen = new java.util.HashSet<>();
		for (ProductRequest.TagRequest req : requests) {
			String group = req.getGroup().trim();
			String value = req.getValue().trim();
			if (!seen.add(group.toLowerCase(java.util.Locale.ROOT) + ":" + value.toLowerCase(java.util.Locale.ROOT))) {
				continue;
			}
			com.ecommerce.catalog.entity.ProductTag tag = p.getTags().stream()
					.filter(t -> t.getGroup().equalsIgnoreCase(group) && t.getValue().equalsIgnoreCase(value))
					.findFirst().orElseGet(com.ecommerce.catalog.entity.ProductTag::new);
			tag.setProduct(p);
			tag.setGroup(group);
			tag.setValue(value);
			tag.setSortOrder(order++);
			keep.add(tag);
		}
		p.getTags().retainAll(keep);
		keep.stream().filter(t -> !p.getTags().contains(t)).forEach(p.getTags()::add);
	}

	private void replaceBox(Product p, List<String> items) {
		p.getBoxItems().clear();
		int order = 0;
		for (String text : items) {
			ProductBoxItem item = new ProductBoxItem();
			item.setProduct(p);
			item.setItem(text.trim());
			item.setSortOrder(order++);
			p.getBoxItems().add(item);
		}
	}

	/**
	 * Images must be ones uploaded here, so a product never points at a host we do not control. A URL already on this
	 * product is kept as it is (products saved before uploads existed still save).
	 */
	private void replaceMedia(Product p, List<ProductRequest.MediaRequest> requests) {
		java.util.Set<String> existing = new java.util.HashSet<>();
		p.getMedia().forEach(m -> existing.add(m.getUrl()));
		for (ProductRequest.MediaRequest req : requests) {
			String url = req.getUrl().trim();
			if (!existing.contains(url) && !imageStorage.isStored(url)) {
				throw ResponseException.badRequest(ErrorCode.IMAGE_NOT_UPLOADED);
			}
		}
		p.getMedia().clear();
		int order = 0;
		for (ProductRequest.MediaRequest req : requests) {
			ProductMedia media = new ProductMedia();
			media.setProduct(p);
			media.setKind(req.getKind());
			media.setUrl(req.getUrl().trim());
			media.setAltText(trim(req.getAltText()));
			media.setSortOrder(order++);
			p.getMedia().add(media);
		}
	}

	private static String trim(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}
}
