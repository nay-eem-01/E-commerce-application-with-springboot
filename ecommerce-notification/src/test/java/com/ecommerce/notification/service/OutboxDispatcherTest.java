package com.ecommerce.notification.service;

import com.ecommerce.notification.Fixtures;
import com.ecommerce.notification.config.EmailSenderRouting;
import com.ecommerce.notification.entity.OutboxEmail;
import com.ecommerce.notification.repository.OutboxEmailRepository;
import com.ecommerce.notification.sender.EmailDeliveryException;
import com.ecommerce.notification.sender.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxDispatcherTest {

	private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");

	private final OutboxEmailRepository repo = mock(OutboxEmailRepository.class);
	private final EmailSender sender = mock(EmailSender.class);
	private OutboxDispatcher dispatcher;
	private OutboxEmail email;

	@BeforeEach
	void setUp() {
		when(sender.id()).thenReturn("log");
		var props = Fixtures.props("log", null);
		dispatcher = new OutboxDispatcher(repo, new EmailSenderRouting(props, List.of(sender), new StandardEnvironment()), props,
				Clock.fixed(NOW, ZoneOffset.UTC), mock(PlatformTransactionManager.class));
		email = new OutboxEmail();
		email.setId(1L);
		email.setDedupKey("SS-1:PLACED");
		email.setRecipient("rafi@example.com");
		email.setSubject("s");
		email.setBodyText("t");
		email.setBodyHtml("h");
		email.setNextAttemptAt(NOW);
		when(repo.lockDue(any(), anyInt())).thenReturn(List.of(email));
		when(repo.findById(1L)).thenReturn(Optional.of(email));
	}

	@Test
	void aDeliveredEmailIsMarkedSent() {
		dispatcher.dispatch();
		assertThat(email.getStatus()).isEqualTo(OutboxEmail.Status.SENT);
		assertThat(email.getAttempts()).isEqualTo(1);
		assertThat(email.getSentAt()).isEqualTo(NOW);
	}

	@Test
	void aFailureBacksOffAndTheFifthFailureGivesUp() {
		doThrow(new EmailDeliveryException("SMTP send failed: MailSendException", null)).when(sender).send(any());
		dispatcher.dispatch();
		assertThat(email.getStatus()).isEqualTo(OutboxEmail.Status.PENDING);
		assertThat(email.getNextAttemptAt()).isEqualTo(NOW.plus(Duration.ofMinutes(1)));
		assertThat(email.getLastError()).contains("MailSendException");

		email.setAttempts(4);
		dispatcher.dispatch();
		assertThat(email.getStatus()).isEqualTo(OutboxEmail.Status.FAILED);
	}

	@Test
	void backoffGrowsThenStaysAtTwoHours() {
		assertThat(OutboxDispatcher.backoff(1)).isEqualTo(Duration.ofMinutes(1));
		assertThat(OutboxDispatcher.backoff(3)).isEqualTo(Duration.ofMinutes(30));
		assertThat(OutboxDispatcher.backoff(9)).isEqualTo(Duration.ofHours(2));
	}
}
