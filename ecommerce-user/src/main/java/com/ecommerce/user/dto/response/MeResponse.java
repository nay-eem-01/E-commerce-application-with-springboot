package com.ecommerce.user.dto.response;

import com.ecommerce.common.phone.PhoneNumbers;
import com.ecommerce.user.entity.User;

import java.util.List;

/** The caller's own profile. Never contains a password hash or another user's data. */
public record MeResponse(
		Long id,
		String name,
		String phone,
		String email,
		List<String> roles,
		boolean hasPassword,
		boolean mustChangePassword
) {
	public static MeResponse from(User user) {
		return new MeResponse(
				user.getId(),
				user.getName(),
				PhoneNumbers.toLocal(user.getPhone()),
				user.getEmail(),
				user.getRoles().stream().map(r -> r.getName().name()).sorted().toList(),
				user.getPasswordHash() != null,
				user.isMustChangePassword()
		);
	}
}
