package com.ecommerce.user.security;

import com.ecommerce.common.security.AuthenticatedUser;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.enums.RoleType;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The authenticated caller. Holds ids and authorities only - no password hash, no
 * personal data - because it lives in the SecurityContext for the whole request.
 */
@Getter
public class UserPrincipal implements AuthenticatedUser {

	private final Long id;
	private final UUID sessionId;
	private final String displayName;
	private final boolean admin;
	private final boolean mustChangePassword;
	private final List<GrantedAuthority> authorities;

	public UserPrincipal(User user, UUID sessionId) {
		this.id = user.getId();
		this.sessionId = sessionId;
		this.displayName = user.getName();
		this.admin = user.hasRole(RoleType.ADMIN);
		this.mustChangePassword = user.isMustChangePassword();
		List<GrantedAuthority> list = new ArrayList<>();
		user.getRoles().forEach(role -> {
			list.add(new SimpleGrantedAuthority("ROLE_" + role.getName().name()));
			role.getPrivileges().forEach(p -> list.add(new SimpleGrantedAuthority(p.getName())));
		});
		this.authorities = List.copyOf(list);
	}

	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}
}
