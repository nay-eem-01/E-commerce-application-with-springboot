package com.ecommerce.common.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The caller's IP for rate limiting. Uses the servlet's remote address only: behind a
 * reverse proxy, set server.forward-headers-strategy=native so Tomcat resolves it from a
 * trusted X-Forwarded-For. Reading the header directly here would let any client pick
 * its own IP and dodge every per-IP limit.
 */
public final class ClientIp {
	private ClientIp() {}

	public static String of(HttpServletRequest request) {
		return request.getRemoteAddr();
	}
}
