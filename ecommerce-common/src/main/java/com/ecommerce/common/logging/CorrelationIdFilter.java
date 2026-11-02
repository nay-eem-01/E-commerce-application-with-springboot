package com.ecommerce.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every request an id that appears in each of its log lines, comes back as a
 * response header, and is quoted in error bodies. Runs first so nothing logs without one.
 * A client-supplied id is accepted only if it looks like an id, so it cannot inject text
 * into log lines.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

	private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
									@NonNull FilterChain chain) throws ServletException, IOException {
		String correlationId = request.getHeader(CorrelationId.HEADER);
		if (correlationId == null || !SAFE_ID.matcher(correlationId).matches()) {
			correlationId = UUID.randomUUID().toString();
		}

		MDC.put(CorrelationId.MDC_KEY, correlationId);
		response.setHeader(CorrelationId.HEADER, correlationId);

		try {
			chain.doFilter(request, response);
		} finally {
			// Pooled threads are reused: a leaked MDC value would tag the next request.
			MDC.remove(CorrelationId.MDC_KEY);
			MDC.remove(CorrelationId.USER_MDC_KEY);
		}
	}
}
