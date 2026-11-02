package com.ecommerce.cart.service;

import com.ecommerce.cart.entity.WishlistItem;
import com.ecommerce.cart.repository.WishlistItemRepository;
import com.ecommerce.catalog.api.CatalogApi;
import com.ecommerce.catalog.dto.response.ProductSummaryResponse;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

	static final int MAX_ITEMS = 200;

	private final WishlistItemRepository wishlistRepository;
	private final CatalogApi catalogApi;

	/** Newest first; archived products drop out silently. */
	@Transactional(readOnly = true)
	public List<ProductSummaryResponse> list(Long userId) {
		List<WishlistItem> items = wishlistRepository.findByUserIdOrderByIdDesc(userId);
		Map<Long, Integer> order = new java.util.HashMap<>();
		for (int i = 0; i < items.size(); i++) {
			order.put(items.get(i).getProductId(), i);
		}
		return catalogApi.summaries(order.keySet()).stream()
				.sorted(Comparator.comparing(s -> order.get(s.id())))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<Long> productIds(Long userId) {
		return wishlistRepository.findByUserIdOrderByIdDesc(userId).stream().map(WishlistItem::getProductId).toList();
	}

	@Transactional
	public void add(Long userId, Long productId) {
		if (wishlistRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
			return;
		}
		Map<Long, ProductSummaryResponse> found = catalogApi.summaries(List.of(productId)).stream()
				.collect(Collectors.toMap(ProductSummaryResponse::id, Function.identity()));
		if (!found.containsKey(productId)) {
			throw ResponseException.notFound(ErrorCode.PRODUCT_NOT_FOUND);
		}
		if (wishlistRepository.countByUserId(userId) >= MAX_ITEMS) {
			throw ResponseException.badRequest(ErrorCode.WISHLIST_LIMIT);
		}
		WishlistItem item = new WishlistItem();
		item.setUserId(userId);
		item.setProductId(productId);
		wishlistRepository.save(item);
	}

	@Transactional
	public void remove(Long userId, Long productId) {
		wishlistRepository.findByUserIdAndProductId(userId, productId).ifPresent(wishlistRepository::delete);
	}
}
