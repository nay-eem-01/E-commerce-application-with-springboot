package com.ecommerce.user.entity;

import com.ecommerce.common.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "addresses")
public class Address extends AuditModel {

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "full_name", nullable = false, length = 80)
	private String fullName;

	@Column(nullable = false, length = 16)
	private String phone;

	@Column(nullable = false, length = 40)
	private String district;

	@Column(nullable = false, length = 80)
	private String area;

	@Column(name = "address_line", nullable = false, length = 250)
	private String addressLine;

	@Column(name = "is_default", nullable = false)
	private boolean defaultAddress;
}
