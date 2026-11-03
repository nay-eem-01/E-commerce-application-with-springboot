package com.ecommerce.catalog.service;

import com.ecommerce.catalog.dto.response.ProductDetailResponse;
import com.ecommerce.catalog.dto.response.ProductSummaryResponse;
import com.ecommerce.catalog.entity.Product;
import com.ecommerce.catalog.enums.ProductStatus;
import com.ecommerce.catalog.repository.ProductRepository;
import com.ecommerce.common.constant.AppConstants;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.model.PageResponse;
import com.ecommerce.common.web.Pages;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.ecommerce.catalog.repository.ProductSpecifications.*;

/** Read side of the storefront. Shoppers only ever see ACTIVE products. */
@Service
@RequiredArgsConstructor
public class ProductQueryService {

	static final Set<String> SORTS = Set.of(AppConstants.SORT_BY_VALUE, "price", "name");

	private final ProductRepository productRepository;
	private final com.ecommerce.catalog.repository.ProductTagRepository productTagRepository;

	public record Filter(String category, String q, Boolean inStock, Boolean preorder, BigDecimal minPrice, BigDecimal maxPrice,
						 String character, List<String> tags) {
		public Filter(String category, String q, Boolean inStock, Boolean preorder, BigDecimal minPrice, BigDecimal maxPrice) {
			this(category, q, inStock, preorder, minPrice, maxPrice, null, List.of());
		}
	}

	/** "Capacity:750 ml" pairs grouped by group; a malformed pair is a 400, not a silent no-op. */
	static java.util.Map<String, List<String>> tagGroups(List<String> tags) {
		java.util.Map<String, List<String>> byGroup = new java.util.LinkedHashMap<>();
		if (tags == null) {
			return byGroup;
		}
		for (String tag : tags) {
			int at = tag == null ? -1 : tag.indexOf(':');
			if (at <= 0 || at == tag.length() - 1) {
				throw ResponseException.badRequest(ErrorCode.TAG_FILTER_INVALID);
			}
			byGroup.computeIfAbsent(tag.substring(0, at).trim(), k -> new ArrayList<>()).add(tag.substring(at + 1).trim());
		}
		return byGroup;
	}

	@Transactional(readOnly = true)
	public com.ecommerce.catalog.dto.response.FacetsResponse facets(String category, String character) {
		java.util.Map<String, List<com.ecommerce.catalog.dto.response.FacetsResponse.Value>> groups = new java.util.LinkedHashMap<>();
		for (Object[] row : productTagRepository.facetCounts(ProductStatus.ACTIVE, category == null ? "" : category,
				character == null ? "" : character)) {
			groups.computeIfAbsent((String) row[0], k -> new ArrayList<>())
					.add(new com.ecommerce.catalog.dto.response.FacetsResponse.Value((String) row[1], (Long) row[2]));
		}
		return new com.ecommerce.catalog.dto.response.FacetsResponse(groups.entrySet().stream()
				.map(e -> new com.ecommerce.catalog.dto.response.FacetsResponse.Group(e.getKey(), e.getValue())).toList());
	}

	@Transactional(readOnly = true)
	public PageResponse<ProductSummaryResponse> list(Filter f, int pageNo, int pageSize, String sortBy, AscOrDescType dir) {
		return find(ProductStatus.ACTIVE, f, pageNo, pageSize, sortBy, dir);
	}

	/** Admin list: any status, or one status when given. */
	@Transactional(readOnly = true)
	public PageResponse<ProductSummaryResponse> find(ProductStatus status, Filter f, int pageNo, int pageSize, String sortBy, AscOrDescType dir) {
		Specification<Product> spec = Specification.allOf(
				status(status), category(f.category()), character(f.character()), tags(tagGroups(f.tags())),
				search(f.q()), inStockOnly(f.inStock()), preorderOnly(f.preorder()), minPrice(f.minPrice()), maxPrice(f.maxPrice()));
		return PageResponse.of(productRepository.findAll(spec, Pages.of(pageNo, pageSize, sortBy, dir, SORTS)),
				ProductSummaryResponse::from);
	}

	@Transactional(readOnly = true)
	public ProductDetailResponse detail(String slug) {
		return ProductDetailResponse.from(activeBySlug(slug));
	}

	/** Same category first, then newest from other categories; up to four. */
	@Transactional(readOnly = true)
	public List<ProductSummaryResponse> related(String slug) {
		Product product = activeBySlug(slug);
		Long categoryId = product.getCategory().getId();
		List<Product> picks = new ArrayList<>(productRepository
				.findTop8ByCategoryIdAndStatusAndIdNotOrderByCreationDateDesc(categoryId, ProductStatus.ACTIVE, product.getId()));
		if (picks.size() < 4) {
			picks.addAll(productRepository.findTop8ByStatusAndCategoryIdNotOrderByCreationDateDesc(ProductStatus.ACTIVE, categoryId));
		}
		return picks.stream().limit(4).map(ProductSummaryResponse::from).toList();
	}

	Product activeBySlug(String slug) {
		return productRepository.findBySlugAndStatus(slug, ProductStatus.ACTIVE)
				.orElseThrow(() -> ResponseException.notFound(ErrorCode.PRODUCT_NOT_FOUND));
	}
}
