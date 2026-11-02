package com.ecommerce.cart.entity;

import com.ecommerce.common.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A line in a signed-in shopper's cart. Holds ids and a quantity only - never a price.
 * The price is read from the catalog every time the cart is shown, and again at checkout.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cart_items")
public class CartItem extends AuditModel {

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "option_id")
	private Long optionId;

	@Column(nullable = false)
	private int quantity;
}
