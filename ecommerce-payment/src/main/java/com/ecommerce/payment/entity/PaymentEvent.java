package com.ecommerce.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * Append-only audit of what happened to a payment (initiated, callback, IPN, verified,
 * refund, expired). A short summary only - never raw payloads, credentials or tokens.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Immutable
@Table(name = "payment_events")
public class PaymentEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "payment_id", nullable = false)
	private Long paymentId;

	@Column(nullable = false, length = 20)
	private String kind;

	@Column(length = 300)
	private String detail;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
}
