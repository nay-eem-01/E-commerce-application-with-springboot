package com.ecommerce.catalog.api;

/** One requested line: product, optional option, quantity. */
public record LineRequest(Long productId, Long optionId, int quantity) {}
