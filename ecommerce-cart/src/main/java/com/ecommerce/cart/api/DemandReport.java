package com.ecommerce.cart.api;

import java.util.Map;

/** Today's demand from members: wishlists and carts. Guests' carts live in their browser and are not counted. */
public interface DemandReport {

	record ProductDemand(Long productId, long wishlists, long cartUnits, long cartMembers) {}

	Map<Long, ProductDemand> byProduct();
}
