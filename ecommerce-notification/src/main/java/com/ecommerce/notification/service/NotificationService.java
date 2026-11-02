package com.ecommerce.notification.service;

import com.ecommerce.common.event.OrderNotificationEvent;
import com.ecommerce.common.logging.LogMasks;
import com.ecommerce.notification.config.MailProperties;
import com.ecommerce.notification.entity.OutboxEmail;
import com.ecommerce.notification.repository.OutboxEmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * Turns order events into outbox rows, inside the transaction that published the event: if the
 * order change rolls back, the email disappears with it. Sending happens later (OutboxDispatcher).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

	private final OutboxEmailRepository outboxRepository;
	private final EmailTemplates templates;
	private final MailProperties properties;
	private final Clock clock;

	@EventListener
	public void on(OrderNotificationEvent event) {
		var order = event.order();
		if (order.email() != null && !order.email().isBlank()) {
			enqueue(order.orderNumber() + ":" + event.type(), order.email(), templates.render(event));
		}
		if (event.type() == OrderNotificationEvent.Type.PLACED && properties.staffAddress() != null && !properties.staffAddress().isBlank()) {
			enqueue(order.orderNumber() + ":STAFF", properties.staffAddress(), templates.staffCopy(order));
		}
	}

	private void enqueue(String dedupKey, String recipient, EmailTemplates.Rendered rendered) {
		if (outboxRepository.existsByDedupKey(dedupKey)) {
			return;
		}
		OutboxEmail email = new OutboxEmail();
		email.setDedupKey(dedupKey);
		email.setRecipient(recipient);
		email.setSubject(rendered.subject());
		email.setBodyText(rendered.text());
		email.setBodyHtml(rendered.html());
		email.setNextAttemptAt(clock.instant());
		outboxRepository.save(email);
		log.info("Queued email {} for {}", dedupKey, LogMasks.maskEmail(recipient));
	}
}
