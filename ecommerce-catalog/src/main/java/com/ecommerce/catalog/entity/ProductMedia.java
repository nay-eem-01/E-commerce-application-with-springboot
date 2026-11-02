package com.ecommerce.catalog.entity;

import com.ecommerce.catalog.enums.MediaKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** An image by URL (CDN or object storage). Upload with byte-sniffing is a roadmap step. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_media")
public class ProductMedia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private MediaKind kind;

	@Column(nullable = false, length = 500)
	private String url;

	@Column(name = "alt_text", length = 160)
	private String altText;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;
}
