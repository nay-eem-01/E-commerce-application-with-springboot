package com.ecommerce.catalog.entity;

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

/** One product inside a set. The set keeps its own price and stock; this is what the box holds. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_bundle_items")
public class ProductBundleItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bundle_id")
	private Product bundle;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "component_id")
	private Product component;

	@Column(nullable = false)
	private int quantity = 1;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;
}
