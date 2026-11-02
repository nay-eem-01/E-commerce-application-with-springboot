package com.ecommerce.common.model;

import com.ecommerce.common.security.AuthenticatedUser;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Stamps the acting user's id. Anonymous requests (guest checkout, OTP sign-in) are
 * stamped SYSTEM - the anonymous token is not a user (AgroTrends fixed the same bug).
 */
public class AuditorAwareImpl implements AuditorAware<String> {

	public static final String SYSTEM = "SYSTEM";

	@Override
	public Optional<String> getCurrentAuditor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return Optional.of(String.valueOf(user.getId()));
		}
		return Optional.of(SYSTEM);
	}
}
