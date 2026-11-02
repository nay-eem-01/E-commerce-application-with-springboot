package com.ecommerce.notification.sender;

import com.ecommerce.common.logging.LogMasks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Writes emails to the log instead of sending them: development, or a deployment where email
 * is deliberately off. Says so loudly at startup (see EmailSenderRouting).
 */
@Slf4j
@Component
public class LoggingEmailSender implements EmailSender {

	public static final String ID = "log";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void send(Email email) {
		log.warn("[DEV EMAIL - not sent] to {} | {}", LogMasks.maskEmail(email.to()), email.subject());
		log.debug("[DEV EMAIL body]\n{}", email.text());
	}
}
