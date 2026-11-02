package com.ecommerce.catalog.repository;

import com.ecommerce.catalog.entity.Category;
import com.ecommerce.catalog.entity.Product;
import com.ecommerce.catalog.enums.ProductStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Filters for the product list and search. Search is a case-insensitive substring match on
 * name, maker, summary, spec line and category name - enough for a catalog of thousands.
 * The upgrade path is a generated tsvector column with a GIN index behind this same method.
 */
public final class ProductSpecifications {
	private ProductSpecifications() {}

	public static Specification<Product> status(ProductStatus status) {
		return (root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status);
	}

	public static Specification<Product> category(String slug) {
		return (root, q, cb) -> slug == null || slug.isBlank() ? null
				: cb.equal(root.join("category", JoinType.INNER).get("slug"), slug);
	}

	public static Specification<Product> inStockOnly(Boolean inStock) {
		return (root, q, cb) -> Boolean.TRUE.equals(inStock) ? cb.greaterThan(root.get("stock"), 0) : null;
	}

	public static Specification<Product> preorderOnly(Boolean preorder) {
		return (root, q, cb) -> preorder == null ? null : cb.equal(root.get("preorder"), preorder);
	}

	public static Specification<Product> minPrice(BigDecimal min) {
		return (root, q, cb) -> min == null ? null : cb.greaterThanOrEqualTo(root.get("price"), min);
	}

	public static Specification<Product> maxPrice(BigDecimal max) {
		return (root, q, cb) -> max == null ? null : cb.lessThanOrEqualTo(root.get("price"), max);
	}

	public static Specification<Product> search(String text) {
		return (root, q, cb) -> {
			if (text == null || text.isBlank()) {
				return null;
			}
			String like = "%" + escape(text.trim().toLowerCase(Locale.ROOT)) + "%";
			Join<Product, Category> category = root.join("category", JoinType.INNER);
			return cb.or(
					cb.like(cb.lower(root.get("name")), like, '\\'),
					cb.like(cb.lower(root.get("maker")), like, '\\'),
					cb.like(cb.lower(root.get("summary")), like, '\\'),
					cb.like(cb.lower(root.get("specLine")), like, '\\'),
					cb.like(cb.lower(category.get("name")), like, '\\')
			);
		};
	}

	/** A shopper typing "100%" searches for the text, not a wildcard. */
	static String escape(String s) {
		return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
