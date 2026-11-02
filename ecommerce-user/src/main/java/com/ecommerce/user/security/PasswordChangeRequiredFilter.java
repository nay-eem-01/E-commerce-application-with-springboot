package com.ecommerce.user.security;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * A seeded admin (or any account flagged by an admin) must set a new password before doing
 * anything else. Only these three endpoints stay reachable until then.
 */
@Component
@RequiredArgsConstructor
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

	private static final Set<String> ALLOWED = Set.of(
			"/api/users/me", "/api/users/me/change-password", "/api/auth/sign-out"
	);

	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
									@NonNull FilterChain chain) throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
				&& principal.isMustChangePassword() && !ALLOWED.contains(request.getRequestURI())) {
			response.setStatus(HttpStatus.FORBIDDEN.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter().write(objectMapper.writeValueAsString(
					GlobalExceptionHandler.build(HttpStatus.FORBIDDEN, ErrorCode.PASSWORD_CHANGE_REQUIRED, null).getBody()));
			return;
		}
		chain.doFilter(request, response);
	}
}
