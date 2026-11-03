package com.ecommerce.catalog.entity;

import com.ecommerce.common.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One of the shop's own characters; products of a character form a collection. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "characters")
@org.hibernate.annotations.BatchSize(size = 50)
public class ShopCharacter extends AuditModel {

	@Column(nullable = false, unique = true, length = 60)
	private String slug;

	@Column(nullable = false, length = 60)
	private String name;

	/** The sign shown on the collection tile, e.g. 狐. */
	@Column(length = 8)
	private String label;

	@Column(length = 160)
	private String blurb;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean active = true;
}
