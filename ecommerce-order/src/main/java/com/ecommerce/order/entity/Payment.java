package com.ecommerce.order.entity;

import com.ecommerce.common.model.AuditModel;
import com.ecommerce.order.enums.PaymentMethod;
import com.ecommerce.order.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** One payment attempt for an order. A separate table so a retry or a refund is a new row. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class Payment extends AuditModel {

	@Column(name = "order_id", nullable = false)
	private Long orderId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private PaymentMethod method;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private PaymentStatus status;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	/** The gateway's transaction id, once there is a gateway. */
	@Column(name = "provider_reference", length = 100)
	private String providerReference;

	@Column(name = "paid_at")
	private Instant paidAt;
}
