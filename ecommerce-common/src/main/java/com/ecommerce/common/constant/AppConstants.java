package com.ecommerce.common.constant;

public final class AppConstants {
	private AppConstants() {}

	public static final String PAGE_NO = "pageNo";
	public static final String PAGE_SIZE = "pageSize";
	public static final String SORT_BY = "sortBy";
	public static final String ASC_OR_DESC = "ascOrDesc";

	public static final String PAGE_NO_VALUE = "0";
	public static final String PAGE_SIZE_VALUE = "20";
	public static final String ASC_OR_DESC_VALUE = "desc";
	public static final String SORT_BY_VALUE = "creationDate";

	/** Every list endpoint clamps pageSize to this. */
	public static final int MAX_PAGE_SIZE = 100;

	public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	public static final String CURRENCY = "BDT";
}
