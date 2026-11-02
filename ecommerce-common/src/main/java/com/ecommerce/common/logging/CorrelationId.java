package com.ecommerce.common.logging;

import org.slf4j.MDC;

public final class CorrelationId {
	private CorrelationId() {}

	public static final String HEADER = "X-Correlation-Id";
	public static final String MDC_KEY = "correlationId";
	public static final String USER_MDC_KEY = "userId";

	/** The id for the request being handled on this thread, or null outside a request. */
	public static String current() {
		return MDC.get(MDC_KEY);
	}
}
