package com.ecommerce.catalog.enums;

public enum StockState {
	IN_STOCK,
	LOW_STOCK,
	SOLD_OUT,
	PREORDER;

	/** "Only 4 left" starts at this many units. */
	public static final int LOW_STOCK_THRESHOLD = 5;

	public static StockState of(boolean preorder, int stock) {
		if (preorder) {
			return stock > 0 ? PREORDER : SOLD_OUT;
		}
		if (stock <= 0) {
			return SOLD_OUT;
		}
		return stock <= LOW_STOCK_THRESHOLD ? LOW_STOCK : IN_STOCK;
	}
}
