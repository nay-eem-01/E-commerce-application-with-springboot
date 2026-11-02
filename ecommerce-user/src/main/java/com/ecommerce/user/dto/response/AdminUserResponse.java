package com.ecommerce.user.dto.response;

import com.ecommerce.common.phone.PhoneNumbers;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.enums.UserStatus;

import java.time.Instant;
import java.util.List;

/** Admin-only view: includes contact details, never the password hash. */
public record AdminUserResponse(
		Long id,
		String name,
		String phone,
		String email,
		UserStatus status,
		List<String> roles,
		Instant creationDate
) {
	public static AdminUserResponse from(User u) {
		return new AdminUserResponse(u.getId(), u.getName(), PhoneNumbers.toLocal(u.getPhone()), u.getEmail(),
				u.getStatus(), u.getRoles().stream().map(r -> r.getName().name()).sorted().toList(), u.getCreationDate());
	}
}
