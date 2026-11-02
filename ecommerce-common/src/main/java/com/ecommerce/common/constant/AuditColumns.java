package com.ecommerce.common.constant;

/** Column names for {@code AuditModel}; every module's tables carry them. */
public final class AuditColumns {
	private AuditColumns() {}

	public static final String ID = "id";
	public static final String CREATED_BY = "created_by";
	public static final String CREATION_DATE = "creation_date";
	public static final String LAST_MODIFIED_BY = "last_modified_by";
	public static final String LAST_MODIFIED_DATE = "last_modified_date";
}
