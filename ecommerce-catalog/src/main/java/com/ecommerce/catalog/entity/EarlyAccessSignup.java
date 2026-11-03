package com.ecommerce.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Someone who asked to hear when a coming-soon category opens. Phone is canonical (+8801...). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "early_access_signups")
public class EarlyAccessSignup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id")
	private Category category;

	@Column(length = 20)
	private String phone;

	@Column(length = 254)
	private String email;

	@Column(name = "creation_date", nullable = false)
	private Instant creationDate = Instant.now();
}
