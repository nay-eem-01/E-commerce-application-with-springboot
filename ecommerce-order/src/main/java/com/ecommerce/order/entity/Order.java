package com.ecommerce.order.entity;

import com.ecommerce.common.model.AuditModel;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.payment.api.PaymentMethod;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * An order is a snapshot: customer, address, item names and prices are copied at placement,
 * so editing a product or an address book entry later never rewrites history. userId is
 * null for a guest order. orderNumber is the public id (URLs, SMS); the numeric id is not.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order extends AuditModel {

	@Column(name = "order_number", nullable = false, unique = true, length = 16, updatable = false)
	private String orderNumber;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "customer_name", nullable = false, length = 80)
	private String customerName;

	@Column(nullable = false, length = 16)
	private String phone;

	@Column(length = 254)
	private String email;

	@Column(nullable = false, length = 40)
	private String district;

	@Column(nullable = false, length = 80)
	private String area;

	@Column(name = "address_line", nullable = false, length = 250)
	private String addressLine;

	@Column(name = "delivery_note", length = 200)
	private String deliveryNote;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private OrderStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 16)
	private PaymentMethod paymentMethod;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2)
	private BigDecimal deliveryFee;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal total;

	/** Sum of pre-order deposits in this order (information for staff and the shopper). */
	@Column(name = "deposit_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal depositTotal;

	@Column(name = "idempotency_key", nullable = false, unique = true, length = 64, updatable = false)
	private String idempotencyKey;

	@Column(name = "placed_at", nullable = false, updatable = false)
	private Instant placedAt;

	@Column(name = "delivered_at")
	private Instant deliveredAt;

	@Version
	@Column(nullable = false)
	private long version;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private List<OrderItem> items = new ArrayList<>();

	@OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
	@OrderBy("id ASC")
	private List<OrderStatusHistory> history = new ArrayList<>();
}
