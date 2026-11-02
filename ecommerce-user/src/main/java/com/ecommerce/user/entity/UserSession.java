package com.ecommerce.user.entity;

import com.ecommerce.common.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One signed-in device. Every access token carries this session's public id ("sid") and is
 * accepted only while the session is active, so sign-out, a ban or a password change take
 * effect immediately instead of when the token expires.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_sessions")
public class UserSession extends AuditModel {

	@Column(name = "public_id", nullable = false, unique = true, updatable = false)
	private UUID publicId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "user_agent", length = 200)
	private String userAgent;

	public boolean isUsable(Instant now) {
		return active && expiresAt.isAfter(now);
	}
}
