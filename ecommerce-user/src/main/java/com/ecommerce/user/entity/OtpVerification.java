package com.ecommerce.user.entity;

import com.ecommerce.common.model.AuditModel;
import com.ecommerce.user.enums.OtpPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A sign-in code. Stored as a BCrypt hash, short-lived, attempt-capped, single use. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "otp_verifications")
public class OtpVerification extends AuditModel {

	@Column(nullable = false, length = 16)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private OtpPurpose purpose;

	@Column(name = "code_hash", nullable = false, length = 100)
	private String codeHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "consumed_at")
	private Instant consumedAt;
}
