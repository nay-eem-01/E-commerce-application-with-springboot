package com.ecommerce.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** productId and optionId are plain ids: the catalog belongs to another module. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_items")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id")
	private Order order;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "option_id")
	private Long optionId;

	@Column(name = "product_slug", nullable = false, length = 120)
	private String productSlug;

	@Column(name = "product_name", nullable = false, length = 140)
	private String productName;

	@Column(name = "option_label", length = 100)
	private String optionLabel;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "line_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal lineTotal;

	@Column(nullable = false)
	private boolean preorder;

	@Column(name = "deposit_per_unit", nullable = false, precision = 12, scale = 2)
	private BigDecimal depositPerUnit;

	@Column(name = "thumbnail_url", length = 500)
	private String thumbnailUrl;
}
