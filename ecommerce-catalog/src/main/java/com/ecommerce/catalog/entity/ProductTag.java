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

/** A filterable attribute: group "Capacity", value "750 ml". */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_tags")
public class ProductTag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(name = "tag_group", nullable = false, length = 30)
	private String group;

	@Column(name = "tag_value", nullable = false, length = 40)
	private String value;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;
}
