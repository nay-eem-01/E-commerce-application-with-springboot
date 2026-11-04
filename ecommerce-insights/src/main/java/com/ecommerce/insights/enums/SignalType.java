package com.ecommerce.insights.enums;

/** In the order the dashboard lists them: stock problems first, then demand, then what to stock next. */
public enum SignalType {
	RESTOCK_SOON,
	WANTED_SOLD_OUT,
	LOW_STOCK_HIGH_DEMAND,
	LOOKED_NOT_BOUGHT,
	SLOW_MOVER,
	SEARCH_GAP,
	COMING_SOON_DEMAND
}
