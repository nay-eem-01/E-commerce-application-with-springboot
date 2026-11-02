package com.ecommerce.notification.config;

import com.ecommerce.notification.sender.EmailSender;
import com.ecommerce.notification.sender.LoggingEmailSender;
import com.ecommerce.notification.sender.SmtpEmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Picks the sender named by app.mail.provider. Fails startup on an unknown provider or on
 * "smtp" without an SMTP host; warns loudly when production only logs emails.
 */
@Slf4j
@Component
public class EmailSenderRouting {

	private final EmailSender active;

	/** Hand-written: validates the configuration and fails startup when it is wrong. */
	public EmailSenderRouting(MailProperties properties, List<EmailSender> senders, Environment environment) {
		this.active = senders.stream().filter(s -> s.id().equals(properties.provider())).findFirst()
				.orElseThrow(() -> new IllegalStateException("app.mail.provider=" + properties.provider()
						+ " is unknown; use one of " + senders.stream().map(EmailSender::id).toList()));
		if (active instanceof SmtpEmailSender smtp && !smtp.isConfigured()) {
			throw new IllegalStateException("MAIL_PROVIDER=smtp but MAIL_HOST is empty; set the SMTP variables (see .env.example)");
		}
		if (active instanceof LoggingEmailSender && environment.acceptsProfiles(Profiles.of("production"))) {
			log.warn("MAIL_PROVIDER=log in production: order emails are written to the log, NOT sent to customers");
		}
		log.info("Email sender: {}", active.id());
	}

	public EmailSender active() {
		return active;
	}
}
