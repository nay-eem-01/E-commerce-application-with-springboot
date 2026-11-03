package com.ecommerce.catalog.dto.response;

import com.ecommerce.catalog.entity.ShopCharacter;

public record CharacterResponse(Long id, String slug, String name, String label, String blurb, int sortOrder, boolean active,
								long productCount) {

	public static CharacterResponse from(ShopCharacter c, long productCount) {
		return new CharacterResponse(c.getId(), c.getSlug(), c.getName(), c.getLabel(), c.getBlurb(), c.getSortOrder(),
				c.isActive(), productCount);
	}
}
