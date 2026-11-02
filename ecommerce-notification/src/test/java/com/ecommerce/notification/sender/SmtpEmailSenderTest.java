package com.ecommerce.notification.sender;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Sends through a real SMTP server (GreenMail, in process) and reads the message back. */
class SmtpEmailSenderTest {

	@RegisterExtension
	static GreenMailExtension smtp = new GreenMailExtension(ServerSetupTest.SMTP);

	@SuppressWarnings("unchecked")
	private static SmtpEmailSender sender(int port) {
		JavaMailSenderImpl mail = new JavaMailSenderImpl();
		mail.setHost("localhost");
		mail.setPort(port);
		ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(mail);
		return new SmtpEmailSender(provider);
	}

	@Test
	void deliversAMultipartTextAndHtmlMessage() throws Exception {
		sender(smtp.getSmtp().getPort()).send(new EmailSender.Email("Senpai Supply <orders@shop.test>", "rafi@example.com",
				"Order SS-7K2M9QXA received - Senpai Supply", "Plain body ৳ 8,400", "<p>HTML body</p>"));

		MimeMessage[] received = smtp.getReceivedMessages();
		assertThat(received).hasSize(1);
		assertThat(received[0].getSubject()).isEqualTo("Order SS-7K2M9QXA received - Senpai Supply");
		assertThat(received[0].getAllRecipients()[0].toString()).isEqualTo("rafi@example.com");
		String raw = GreenMailUtil.getWholeMessage(received[0]);
		assertThat(raw).contains("multipart/").contains("text/plain").contains("text/html");
	}

	@Test
	void anUnreachableServerIsADeliveryFailureNotACrash() {
		assertThatThrownBy(() -> sender(1).send(new EmailSender.Email("a@shop.test", "b@example.com", "s", "t", "<p>h</p>")))
				.isInstanceOf(EmailDeliveryException.class);
	}
}
