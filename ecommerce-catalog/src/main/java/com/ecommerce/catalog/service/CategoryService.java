package com.ecommerce.catalog.service;

import com.ecommerce.catalog.dto.request.CategoryRequest;
import com.ecommerce.catalog.dto.response.CategoryResponse;
import com.ecommerce.catalog.entity.Category;
import com.ecommerce.catalog.enums.ProductStatus;
import com.ecommerce.catalog.repository.CategoryRepository;
import com.ecommerce.catalog.repository.ProductRepository;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;

	@Transactional(readOnly = true)
	public List<CategoryResponse> listActive() {
		Map<Long, Long> counts = activeCounts();
		return categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
				.map(c -> CategoryResponse.from(c, counts.getOrDefault(c.getId(), 0L))).toList();
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> listAll() {
		Map<Long, Long> counts = activeCounts();
		return categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
				.map(c -> CategoryResponse.from(c, counts.getOrDefault(c.getId(), 0L))).toList();
	}

	@Transactional
	public CategoryResponse create(CategoryRequest request) {
		if (categoryRepository.existsBySlug(request.getSlug())) {
			throw ResponseException.conflict(ErrorCode.CATEGORY_SLUG_TAKEN);
		}
		Category category = new Category();
		apply(category, request);
		return CategoryResponse.from(categoryRepository.save(category), 0);
	}

	@Transactional
	public CategoryResponse update(Long id, CategoryRequest request) {
		Category category = find(id);
		if (categoryRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
			throw ResponseException.conflict(ErrorCode.CATEGORY_SLUG_TAKEN);
		}
		apply(category, request);
		return CategoryResponse.from(category, activeCounts().getOrDefault(id, 0L));
	}

	@Transactional
	public void delete(Long id) {
		Category category = find(id);
		if (productRepository.existsByCategoryId(id)) {
			throw ResponseException.conflict(ErrorCode.CATEGORY_NOT_EMPTY);
		}
		categoryRepository.delete(category);
	}

	Category find(Long id) {
		return categoryRepository.findById(id).orElseThrow(() -> ResponseException.notFound(ErrorCode.CATEGORY_NOT_FOUND));
	}

	private Map<Long, Long> activeCounts() {
		Map<Long, Long> counts = new HashMap<>();
		for (Object[] row : productRepository.countByCategory(ProductStatus.ACTIVE)) {
			counts.put((Long) row[0], (Long) row[1]);
		}
		return counts;
	}

	private void apply(Category category, CategoryRequest request) {
		category.setSlug(request.getSlug());
		category.setName(request.getName().trim());
		category.setLabel(blankToNull(request.getLabel()));
		category.setBlurb(blankToNull(request.getBlurb()));
		category.setSortOrder(request.getSortOrder());
		category.setActive(request.isActive());
		category.setComingSoon(request.isComingSoon());
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}
}
