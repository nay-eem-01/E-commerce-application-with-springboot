package com.ecommerce.notification.sender;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * SMTP through Spring Mail (spring.mail.* from MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD).
 * Sends a multipart message: plain text plus HTML, so every client shows something readable.
 */
@Component
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

	public static final String ID = "smtp";

	private final ObjectProvider<JavaMailSender> mailSender;

	@Override
	public String id() {
		return ID;
	}

	public boolean isConfigured() {
		return mailSender.getIfAvailable() != null;
	}

	@Override
	public void send(Email email) {
		JavaMailSender sender = mailSender.getIfAvailable();
		if (sender == null) {
			throw new EmailDeliveryException("SMTP is not configured (MAIL_HOST is empty)", null);
		}
		try {
			MimeMessage message = sender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
			helper.setFrom(email.from());
			helper.setTo(email.to());
			helper.setSubject(email.subject());
			helper.setText(email.text(), email.html());
			sender.send(message);
		} catch (MessagingException | MailException e) {
			throw new EmailDeliveryException("SMTP send failed: " + e.getClass().getSimpleName(), e);
		}
	}
}
