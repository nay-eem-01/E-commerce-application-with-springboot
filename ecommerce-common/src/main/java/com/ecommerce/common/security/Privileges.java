package com.ecommerce.common.security;

import java.util.List;

/**
 * Every privilege used in a {@code @PreAuthorize}. Seeded by Flyway and granted to ADMIN.
 * Each check names the privilege of that resource - never a copy-pasted neighbour.
 */
public final class Privileges {
	private Privileges() {}

	public static final String CATEGORY_WRITE = "CATEGORY_WRITE";
	public static final String PRODUCT_WRITE = "PRODUCT_WRITE";
	public static final String PRODUCT_READ_ALL = "PRODUCT_READ_ALL";
	public static final String ORDER_READ_ALL = "ORDER_READ_ALL";
	public static final String ORDER_UPDATE = "ORDER_UPDATE";
	public static final String USER_READ = "USER_READ";
	public static final String USER_UPDATE = "USER_UPDATE";
	public static final String REVIEW_MODERATE = "REVIEW_MODERATE";

	public static final List<String> ALL = List.of(
			CATEGORY_WRITE, PRODUCT_WRITE, PRODUCT_READ_ALL, ORDER_READ_ALL,
			ORDER_UPDATE, USER_READ, USER_UPDATE, REVIEW_MODERATE
	);
}
