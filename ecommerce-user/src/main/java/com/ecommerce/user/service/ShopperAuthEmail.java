package com.ecommerce.user.service;

/** In-memory event so the raw link never goes into the order outbox. */
record ShopperAuthEmail(String to, String subject, String body) {

	@Override
	public String toString() {
		return "ShopperAuthEmail[redacted]";
	}
}
