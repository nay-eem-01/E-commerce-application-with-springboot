package com.ecommerce.notification.entity;

import com.ecommerce.common.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * An email waiting to be sent, rendered when it was queued. dedupKey (e.g. "SS-7K2M9QXA:SHIPPED")
 * stops the same message being queued twice. Sent rows are deleted after app.mail.retention.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_outbox")
public class OutboxEmail extends AuditModel {

	public enum Status { PENDING, SENT, FAILED }

	@Column(name = "dedup_key", nullable = false, unique = true, length = 100, updatable = false)
	private String dedupKey;

	@Column(nullable = false, length = 254)
	private String recipient;

	@Column(nullable = false, length = 200)
	private String subject;

	@Column(name = "body_text", nullable = false, columnDefinition = "TEXT")
	private String bodyText;

	@Column(name = "body_html", nullable = false, columnDefinition = "TEXT")
	private String bodyHtml;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private Status status = Status.PENDING;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "next_attempt_at", nullable = false)
	private Instant nextAttemptAt;

	@Column(name = "last_error", length = 300)
	private String lastError;

	@Column(name = "sent_at")
	private Instant sentAt;
}
