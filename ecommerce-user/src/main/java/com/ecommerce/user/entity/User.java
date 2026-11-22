package com.ecommerce.user.entity;

import com.ecommerce.common.model.AuditModel;
import com.ecommerce.user.enums.RoleType;
import com.ecommerce.user.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Customers sign in with email and password or Google. Legacy phone accounts remain in the
 * database. The database checks that each account has at least one sign-in identity.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@DynamicUpdate
@Table(name = "users")
public class User extends AuditModel {

	@Column(length = 80)
	private String name;

	/** Canonical +8801XXXXXXXXX. */
	@Column(unique = true, length = 16)
	private String phone;

	@Column(unique = true, length = 254)
	private String email;

	@Column(name = "password_hash", length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "email_verified_at")
	private Instant emailVerifiedAt;

	@Column(name = "google_subject", unique = true)
	private String googleSubject;

	@Column(name = "phone_verified_at")
	private Instant phoneVerifiedAt;

	@Column(name = "must_change_password", nullable = false)
	private boolean mustChangePassword;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
			name = "users_roles",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id")
	)
	private Set<Role> roles = new HashSet<>();

	public boolean hasRole(RoleType type) {
		return roles.stream().anyMatch(role -> role.getName() == type);
	}
}
