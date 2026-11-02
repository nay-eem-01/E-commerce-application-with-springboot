package com.ecommerce.payment.entity;

import com.ecommerce.common.model.AuditModel;
import com.ecommerce.payment.api.PaymentMethod;
import com.ecommerce.payment.api.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One payment attempt for an order. A retry is a new row. {@code provider} is stored per row,
 * so switching a method's route never orphans payments started with the previous vendor.
 * {@code @Version}: a browser callback and the vendor's IPN can arrive at the same moment.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class Payment extends AuditModel {

	@Column(name = "order_id", nullable = false)
	private Long orderId;

	/** Snapshot so callbacks can send the shopper back to the right order page. */
	@Column(name = "order_number", nullable = false, length = 16)
	private String orderNumber;

	/** Our public id for the attempt, sent to the vendor as invoice / transaction id. */
	@Column(nullable = false, unique = true, length = 40, updatable = false)
	private String reference;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private PaymentMethod method;

	@Column(nullable = false, length = 32)
	private String provider;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private PaymentStatus status;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency = "BDT";

	@Column(name = "provider_payment_id", length = 100)
	private String providerPaymentId;

	@Column(name = "provider_transaction_id", length = 100)
	private String providerTransactionId;

	@Column(name = "failure_reason", length = 200)
	private String failureReason;

	@Column(name = "paid_at")
	private Instant paidAt;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(name = "refunded_at")
	private Instant refundedAt;

	@Version
	@Column(nullable = false)
	private long version;
}
