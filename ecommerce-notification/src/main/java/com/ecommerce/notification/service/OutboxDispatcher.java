package com.ecommerce.notification.service;

import com.ecommerce.common.logging.LogMasks;
import com.ecommerce.notification.config.EmailSenderRouting;
import com.ecommerce.notification.config.MailProperties;
import com.ecommerce.notification.entity.OutboxEmail;
import com.ecommerce.notification.repository.OutboxEmailRepository;
import com.ecommerce.notification.sender.EmailDeliveryException;
import com.ecommerce.notification.sender.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Sends due outbox emails. Claiming takes a short lease (next_attempt_at = now + 5 min) under
 * FOR UPDATE SKIP LOCKED, so several instances never send the same email and a crash mid-send
 * only delays it. The SMTP call runs outside any transaction. Failures back off 1 min, 5 min,
 * 30 min, 2 h; after 5 attempts the email is FAILED and visible in the admin list for a retry.
 */
@Slf4j
@Service
public class OutboxDispatcher {

	static final int BATCH = 20;
	static final int MAX_ATTEMPTS = 5;
	static final Duration LEASE = Duration.ofMinutes(5);
	private static final Duration[] BACKOFF = {
			Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30), Duration.ofHours(2)
	};

	private final OutboxEmailRepository outboxRepository;
	private final EmailSenderRouting routing;
	private final MailProperties properties;
	private final Clock clock;
	private final TransactionTemplate tx;

	/** Hand-written: builds the REQUIRES_NEW template around each claim and result. */
	public OutboxDispatcher(OutboxEmailRepository outboxRepository, EmailSenderRouting routing, MailProperties properties,
							Clock clock, PlatformTransactionManager transactionManager) {
		this.outboxRepository = outboxRepository;
		this.routing = routing;
		this.properties = properties;
		this.clock = clock;
		this.tx = new TransactionTemplate(transactionManager);
		this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@Scheduled(fixedDelayString = "${app.mail.dispatch-interval:PT15S}", initialDelayString = "PT10S")
	public void dispatch() {
		Instant now = clock.instant();
		List<OutboxEmail> batch = tx.execute(s -> {
			List<OutboxEmail> due = outboxRepository.lockDue(now, BATCH);
			due.forEach(e -> {
				e.setAttempts(e.getAttempts() + 1);
				e.setNextAttemptAt(now.plus(LEASE));
			});
			return due;
		});
		EmailSender sender = routing.active();
		for (OutboxEmail e : batch) {
			try {
				sender.send(new EmailSender.Email(properties.from(), e.getRecipient(), e.getSubject(), e.getBodyText(), e.getBodyHtml()));
				tx.executeWithoutResult(s -> outboxRepository.findById(e.getId()).ifPresent(fresh -> {
					fresh.setStatus(OutboxEmail.Status.SENT);
					fresh.setSentAt(clock.instant());
					fresh.setLastError(null);
				}));
			} catch (EmailDeliveryException ex) {
				tx.executeWithoutResult(s -> outboxRepository.findById(e.getId()).ifPresent(fresh -> {
					fresh.setLastError(ex.getMessage() == null ? "delivery failed" : ex.getMessage().substring(0, Math.min(300, ex.getMessage().length())));
					if (fresh.getAttempts() >= MAX_ATTEMPTS) {
						fresh.setStatus(OutboxEmail.Status.FAILED);
						log.error("Email {} to {} FAILED after {} attempts: {}", fresh.getDedupKey(), LogMasks.maskEmail(fresh.getRecipient()), fresh.getAttempts(), ex.getMessage());
					} else {
						fresh.setNextAttemptAt(clock.instant().plus(backoff(fresh.getAttempts())));
						log.warn("Email {} attempt {} failed; retrying later: {}", fresh.getDedupKey(), fresh.getAttempts(), ex.getMessage());
					}
				}));
			}
		}
		tx.executeWithoutResult(s -> outboxRepository.deleteSentBefore(clock.instant().minus(properties.retention())));
	}

	static Duration backoff(int attempts) {
		return BACKOFF[Math.min(Math.max(attempts, 1), BACKOFF.length) - 1];
	}
}
