package com.ecommerce.common.event;

import java.util.List;

/**
 * Published by the order module inside the placing transaction. Listeners (cart clears the
 * bought lines, user saves the first address) run synchronously in the same transaction,
 * so a listener failure rolls the order back. Lives in common so listeners need no
 * dependency on the order module; a broker can replace this later without changing callers.
 */
public record OrderPlacedEvent(
		Long orderId,
		String orderNumber,
		Long userId,
		List<Line> lines,
		Address address,
		boolean saveAddress
) {
	public record Line(Long productId, Long optionId, int quantity) {}

	public record Address(String fullName, String phone, String district, String area, String addressLine) {}
}
