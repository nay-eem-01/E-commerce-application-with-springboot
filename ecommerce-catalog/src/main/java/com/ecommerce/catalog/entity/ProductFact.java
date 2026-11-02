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

/** A spec row: "Height" / "24 cm with base". The first four fill the product page's fact grid. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_facts")
public class ProductFact {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(nullable = false, length = 40)
	private String label;

	@Column(name = "fact_value", nullable = false, length = 120)
	private String value;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;
}
