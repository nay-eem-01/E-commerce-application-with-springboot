package com.ecommerce.common.security;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Single place that answers "who is calling?". Identity always comes from the validated
 * access token, never from a request body, path or query parameter.
 */
@Component
public class CurrentUser {

	public Optional<AuthenticatedUser> find() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return Optional.of(user);
		}
		return Optional.empty();
	}

	public AuthenticatedUser require() {
		return find().orElseThrow(() -> ResponseException.unauthorized(ErrorCode.UNAUTHORIZED));
	}

	public Long id() {
		return require().getId();
	}

	/** Null for guests - used by endpoints that work both signed in and as a guest. */
	public Long idOrNull() {
		return find().map(AuthenticatedUser::getId).orElse(null);
	}

	public boolean isAdmin() {
		return find().map(AuthenticatedUser::isAdmin).orElse(false);
	}

	/** Passes for the owner of a resource or any admin; otherwise 403. */
	public void assertOwnerOrAdmin(Long ownerUserId) {
		AuthenticatedUser user = require();
		if (user.isAdmin() || (ownerUserId != null && ownerUserId.equals(user.getId()))) {
			return;
		}
		throw ResponseException.forbidden(ErrorCode.FORBIDDEN);
	}
}
