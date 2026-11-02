package com.ecommerce.user.security;

import com.ecommerce.common.logging.CorrelationId;
import com.ecommerce.user.enums.UserStatus;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Authenticates a request from its bearer token. A token is accepted only if it is a valid
 * access JWT, its session is active and its user is not banned. Anything else leaves the
 * request anonymous; protected endpoints then answer 401 through the entry point.
 * A bad token never causes a 500.
 *
 * Costs two primary-key lookups per authenticated request. Cache them (short TTL, evicted
 * on revoke) when traffic needs it - the filter is the only place to change.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER = "Bearer ";

	private final JwtTokenProvider tokenProvider;
	private final UserSessionRepository sessionRepository;
	private final UserRepository userRepository;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
									@NonNull FilterChain chain) throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith(BEARER)) {
			tokenProvider.parse(header.substring(BEARER.length()).trim())
					.flatMap(claims -> sessionRepository.findByPublicId(claims.sessionId())
							.filter(session -> session.isUsable(Instant.now()))
							.filter(session -> session.getUserId().equals(claims.userId()))
							.flatMap(session -> userRepository.findById(claims.userId()))
							.filter(user -> user.getStatus() == UserStatus.ACTIVE)
							.map(user -> new UserPrincipal(user, claims.sessionId())))
					.ifPresent(this::authenticate);
		}
		chain.doFilter(request, response);
	}

	private void authenticate(UserPrincipal principal) {
		UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
		MDC.put(CorrelationId.USER_MDC_KEY, String.valueOf(principal.getId()));
	}
}
