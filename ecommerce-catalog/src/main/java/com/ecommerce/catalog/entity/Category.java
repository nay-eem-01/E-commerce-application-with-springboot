package com.ecommerce.catalog.entity;

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
@Table(name = "categories")
public class Category extends AuditModel {

	@Column(nullable = false, unique = true, length = 60)
	private String slug;

	@Column(nullable = false, length = 60)
	private String name;

	/** Short display mark, e.g. the kanji shown on the category tile. */
	@Column(length = 8)
	private String label;

	@Column(length = 160)
	private String blurb;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean active = true;
}
