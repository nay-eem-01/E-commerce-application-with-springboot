package com.ecommerce.catalog.dto.response;

import com.ecommerce.catalog.entity.Category;

public record CategoryResponse(Long id, String slug, String name, String label, String blurb, int sortOrder, boolean active, long productCount,
							   boolean comingSoon) {

	public static CategoryResponse from(Category c, long productCount) {
		return new CategoryResponse(c.getId(), c.getSlug(), c.getName(), c.getLabel(), c.getBlurb(), c.getSortOrder(), c.isActive(), productCount, c.isComingSoon());
	}
}
