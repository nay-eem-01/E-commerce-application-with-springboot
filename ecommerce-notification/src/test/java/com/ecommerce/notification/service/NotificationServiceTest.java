package com.ecommerce.notification.service;

import com.ecommerce.common.event.OrderNotificationEvent.Type;
import com.ecommerce.notification.Fixtures;
import com.ecommerce.notification.entity.OutboxEmail;
import com.ecommerce.notification.repository.OutboxEmailRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

	private final OutboxEmailRepository repo = mock(OutboxEmailRepository.class);

	private NotificationService service(String staff) {
		var props = Fixtures.props("log", staff);
		return new NotificationService(repo, new EmailTemplates(props), props, Clock.systemUTC());
	}

	@Test
	void queuesTheShopperEmailAndAStaffCopyForNewOrders() {
		service("staff@shop.test").on(Fixtures.event(Type.PLACED, "rafi@example.com", "COD", false, "Rafi"));
		ArgumentCaptor<OutboxEmail> saved = ArgumentCaptor.forClass(OutboxEmail.class);
		verify(repo, times(2)).save(saved.capture());
		assertThat(saved.getAllValues()).extracting(OutboxEmail::getDedupKey).containsExactly("SS-7K2M9QXA:PLACED", "SS-7K2M9QXA:STAFF");
		assertThat(saved.getAllValues()).extracting(OutboxEmail::getRecipient).containsExactly("rafi@example.com", "staff@shop.test");
		assertThat(saved.getValue().getStatus()).isEqualTo(OutboxEmail.Status.PENDING);
	}

	@Test
	void shoppersWithoutAnEmailGetNothingAndStatusChangesHaveNoStaffCopy() {
		service("staff@shop.test").on(Fixtures.event(Type.SHIPPED, null, "COD", false, "Rafi"));
		verify(repo, never()).save(any());
	}

	@Test
	void theSameMessageIsNeverQueuedTwice() {
		when(repo.existsByDedupKey("SS-7K2M9QXA:SHIPPED")).thenReturn(true);
		service(null).on(Fixtures.event(Type.SHIPPED, "rafi@example.com", "COD", false, "Rafi"));
		verify(repo, never()).save(any());
	}
}
