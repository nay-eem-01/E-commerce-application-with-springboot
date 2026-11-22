package com.ecommerce.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
class ShopperAuthEmailSender {

	private final ObjectProvider<JavaMailSender> mail;

	@Value("${app.mail.from}")
	private String from;

	@TransactionalEventListener
	public void send(ShopperAuthEmail email) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(email.to());
		message.setSubject(email.subject());
		message.setText(email.body());
		try {
			mail.getObject().send(message);
		} catch (MailException e) {
			// SMTP exceptions can include recipient addresses. The shopper can request a new link.
			log.warn("Shopper auth email delivery failed with {}", e.getClass().getSimpleName());
		}
	}
}
