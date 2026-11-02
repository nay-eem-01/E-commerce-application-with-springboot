package com.ecommerce.cart.entity;

import com.ecommerce.common.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "wishlist_items")
public class WishlistItem extends AuditModel {

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "product_id", nullable = false)
	private Long productId;
}
