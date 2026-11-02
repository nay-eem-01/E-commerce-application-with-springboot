package com.ecommerce.cart.listener;

import com.ecommerce.cart.service.CartService;
import com.ecommerce.common.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Takes the bought lines out of the signed-in cart, inside the order transaction. */
@Component
@RequiredArgsConstructor
public class OrderPlacedCartListener {

	private final CartService cartService;

	@EventListener
	public void on(OrderPlacedEvent event) {
		if (event.userId() == null) {
			return;
		}
		event.lines().forEach(line -> cartService.removeBought(event.userId(), line.productId(), line.optionId()));
	}
}
