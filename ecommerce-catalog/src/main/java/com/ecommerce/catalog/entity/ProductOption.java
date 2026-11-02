package com.ecommerce.catalog.entity;

import com.ecommerce.common.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One choice in the product's option group (a size, a colour, an edition). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_options")
public class ProductOption extends AuditModel {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(name = "option_value", nullable = false, length = 60)
	private String value;

	/** Added to the product price; zero for most options. */
	@Column(name = "price_delta", nullable = false, precision = 12, scale = 2)
	private BigDecimal priceDelta = BigDecimal.ZERO;

	@Column(nullable = false)
	private int stock;

	/** CSS colour for a swatch chip, e.g. "#f4a6c0". Null for text chips. */
	@Column(length = 32)
	private String swatch;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;
}
