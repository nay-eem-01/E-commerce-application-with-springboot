package com.ecommerce.catalog.dto.response;

import com.ecommerce.catalog.entity.EarlyAccessSignup;
import com.ecommerce.common.phone.PhoneNumbers;

import java.time.Instant;

/** Admin view of a signup. */
public record EarlyAccessSignupResponse(Long id, String categorySlug, String phone, String email, Instant creationDate) {
	public static EarlyAccessSignupResponse from(EarlyAccessSignup s) {
		return new EarlyAccessSignupResponse(s.getId(), s.getCategory().getSlug(),
				s.getPhone() == null ? null : PhoneNumbers.toLocal(s.getPhone()), s.getEmail(), s.getCreationDate());
	}
}
