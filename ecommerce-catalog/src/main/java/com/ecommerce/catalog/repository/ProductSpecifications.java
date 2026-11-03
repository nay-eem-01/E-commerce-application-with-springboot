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

	public static Specification<Product> character(String slug) {
		return (root, q, cb) -> slug == null || slug.isBlank() ? null
				: cb.equal(root.join("character", JoinType.INNER).get("slug"), slug);
	}

	/**
	 * Tag filters as "Group:Value". Values of one group widen the list (750 ml or 1 L); groups
	 * narrow it (750 ml and Steel). Each group is one EXISTS subquery, so products never repeat.
	 */
	public static Specification<Product> tags(java.util.Map<String, java.util.List<String>> byGroup) {
		return (root, query, cb) -> {
			if (byGroup == null || byGroup.isEmpty()) {
				return null;
			}
			java.util.List<jakarta.persistence.criteria.Predicate> all = new java.util.ArrayList<>();
			for (var e : byGroup.entrySet()) {
				var sub = query.subquery(Long.class);
				var t = sub.from(com.ecommerce.catalog.entity.ProductTag.class);
				sub.select(t.get("id")).where(cb.equal(t.get("product"), root), cb.equal(t.get("group"), e.getKey()),
						t.get("value").in(e.getValue()));
				all.add(cb.exists(sub));
			}
			return cb.and(all.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
	}

	/** true: only sets; false: only ordinary products; null: both. */
	public static Specification<Product> setsOnly(Boolean set) {
		return (root, q, cb) -> set == null ? null
				: set ? cb.isNotEmpty(root.get("bundleItems")) : cb.isEmpty(root.get("bundleItems"));
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
