package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.request.CartLineRequest;
import com.ecommerce.cart.dto.response.CartResponse;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.catalog.api.CatalogApi;
import com.ecommerce.catalog.api.LineRequest;
import com.ecommerce.catalog.api.PricedLine;
import com.ecommerce.catalog.enums.StockState;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** The signed-in cart. Guests keep their cart in the browser and send it at checkout. */
@Service
@RequiredArgsConstructor
public class CartService {

	static final int MAX_LINES = 50;
	static final int MAX_QUANTITY = 10;

	private final CartItemRepository cartItemRepository;
	private final CatalogApi catalogApi;

	@Transactional(readOnly = true)
	public CartResponse get(Long userId) {
		List<CartItem> items = cartItemRepository.findByUserIdOrderByIdAsc(userId);
		List<CartResponse.Line> lines = new ArrayList<>();
		BigDecimal subtotal = Money.ZERO;
		int count = 0;
		for (CartItem item : items) {
			PricedLine priced = priceOrNull(item.getProductId(), item.getOptionId(), item.getQuantity());
			if (priced == null) {
				lines.add(new CartResponse.Line(item.getId(), item.getProductId(), item.getOptionId(), null, null, null, null,
						null, item.getQuantity(), null, StockState.SOLD_OUT, false, false, null, null));
				continue;
			}
			BigDecimal lineTotal = Money.times(priced.unitPrice(), item.getQuantity());
			subtotal = Money.add(subtotal, lineTotal);
			count += item.getQuantity();
			lines.add(new CartResponse.Line(item.getId(), priced.productId(), priced.optionId(), priced.slug(), priced.name(),
					priced.optionName(), priced.optionValue(), priced.unitPrice(), item.getQuantity(), lineTotal,
					priced.stockState(), true, priced.preorder(), priced.preorderRelease(), priced.thumbnailUrl()));
		}
		return new CartResponse(lines, count, subtotal);
	}

	@Transactional
	public CartResponse setLine(Long userId, CartLineRequest request) {
		upsert(userId, request.getProductId(), request.getOptionId(), request.getQuantity(), false);
		return get(userId);
	}

	@Transactional
	public CartResponse removeLine(Long userId, Long itemId) {
		CartItem item = cartItemRepository.findByIdAndUserId(itemId, userId)
				.orElseThrow(() -> ResponseException.notFound(ErrorCode.CART_ITEM_NOT_FOUND));
		cartItemRepository.delete(item);
		return get(userId);
	}

	/** Adds the browser cart to the account cart. Lines that no longer exist are skipped, not errors. */
	@Transactional
	public CartResponse merge(Long userId, List<CartLineRequest> lines) {
		for (CartLineRequest line : lines) {
			if (line.getQuantity() <= 0) {
				continue;
			}
			try {
				upsert(userId, line.getProductId(), line.getOptionId(), line.getQuantity(), true);
			} catch (ResponseException skipped) {
				// archived product, removed option or full cart: keep merging the rest
			}
		}
		return get(userId);
	}

	@Transactional
	public void clear(Long userId) {
		cartItemRepository.deleteByUserId(userId);
	}

	/** Removes what was just ordered; called from the order-placed listener. */
	@Transactional
	public void removeBought(Long userId, Long productId, Long optionId) {
		cartItemRepository.findLine(userId, productId, optionId).ifPresent(cartItemRepository::delete);
	}

	private void upsert(Long userId, Long productId, Long optionId, int quantity, boolean add) {
		CartItem existing = cartItemRepository.findLine(userId, productId, optionId).orElse(null);
		if (quantity == 0 && !add) {
			if (existing != null) {
				cartItemRepository.delete(existing);
			}
			return;
		}
		// Validates the product is active and the option belongs to it (throws otherwise).
		catalogApi.price(List.of(new LineRequest(productId, optionId, 1)));

		if (existing == null) {
			if (cartItemRepository.countByUserId(userId) >= MAX_LINES) {
				throw ResponseException.badRequest(ErrorCode.CART_LIMIT);
			}
			existing = new CartItem();
			existing.setUserId(userId);
			existing.setProductId(productId);
			existing.setOptionId(optionId);
			existing.setQuantity(0);
		}
		int next = add ? existing.getQuantity() + quantity : quantity;
		existing.setQuantity(Math.min(MAX_QUANTITY, next));
		cartItemRepository.save(existing);
	}

	private PricedLine priceOrNull(Long productId, Long optionId, int quantity) {
		try {
			return catalogApi.price(List.of(new LineRequest(productId, optionId, quantity))).getFirst();
		} catch (ResponseException unavailable) {
			return null;
		}
	}
}
