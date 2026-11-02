package com.ecommerce.notification.dto;

import com.ecommerce.notification.entity.OutboxEmail;

import java.time.Instant;

/** Admin view of a queued email (no body: it holds the customer's order details). */
public record NotificationResponse(Long id, String dedupKey, String recipient, String subject, OutboxEmail.Status status,
								   int attempts, Instant nextAttemptAt, String lastError, Instant sentAt, Instant creationDate) {

	public static NotificationResponse from(OutboxEmail e) {
		return new NotificationResponse(e.getId(), e.getDedupKey(), e.getRecipient(), e.getSubject(), e.getStatus(),
				e.getAttempts(), e.getNextAttemptAt(), e.getLastError(), e.getSentAt(), e.getCreationDate());
	}
}
