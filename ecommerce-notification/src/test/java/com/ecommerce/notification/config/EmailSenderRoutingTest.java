package com.ecommerce.notification.config;

import com.ecommerce.notification.Fixtures;
import com.ecommerce.notification.sender.LoggingEmailSender;
import com.ecommerce.notification.sender.SmtpEmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailSenderRoutingTest {

	@SuppressWarnings("unchecked")
	private static SmtpEmailSender smtp(JavaMailSender sender) {
		ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(sender);
		return new SmtpEmailSender(provider);
	}

	@Test
	void theSenderIsChosenByProperty() {
		var senders = List.of(new LoggingEmailSender(), smtp(new JavaMailSenderImpl()));
		assertThat(new EmailSenderRouting(Fixtures.props("log", null), senders, new StandardEnvironment()).active().id()).isEqualTo("log");
		assertThat(new EmailSenderRouting(Fixtures.props("smtp", null), senders, new StandardEnvironment()).active().id()).isEqualTo("smtp");
	}

	@Test
	void smtpWithoutAHostOrAnUnknownProviderStopsStartup() {
		var senders = List.of(new LoggingEmailSender(), smtp(null));
		assertThatThrownBy(() -> new EmailSenderRouting(Fixtures.props("smtp", null), senders, new StandardEnvironment()))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("MAIL_HOST");
		assertThatThrownBy(() -> new EmailSenderRouting(Fixtures.props("carrier-pigeon", null), senders, new StandardEnvironment()))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("unknown");
	}
}
