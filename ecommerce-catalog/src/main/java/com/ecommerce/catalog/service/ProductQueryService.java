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

	public record Filter(String category, String q, Boolean inStock, Boolean preorder, BigDecimal minPrice, BigDecimal maxPrice) {}

	@Transactional(readOnly = true)
	public PageResponse<ProductSummaryResponse> list(Filter f, int pageNo, int pageSize, String sortBy, AscOrDescType dir) {
		return find(ProductStatus.ACTIVE, f, pageNo, pageSize, sortBy, dir);
	}

	/** Admin list: any status, or one status when given. */
	@Transactional(readOnly = true)
	public PageResponse<ProductSummaryResponse> find(ProductStatus status, Filter f, int pageNo, int pageSize, String sortBy, AscOrDescType dir) {
		Specification<Product> spec = Specification.allOf(
				status(status), category(f.category()), search(f.q()), inStockOnly(f.inStock()),
				preorderOnly(f.preorder()), minPrice(f.minPrice()), maxPrice(f.maxPrice()));
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
