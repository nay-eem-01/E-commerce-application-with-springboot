package com.ecommerce.user.sms;

import com.ecommerce.user.config.SmsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Development only: writes the SMS (including the code) to the log, loudly, so the OTP
 * flow works without a provider. This is the one deliberate exception to "never log a
 * code". It refuses to run outside the development profile.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsoleSmsSender implements SmsSender {

	private final SmsProperties properties;
	private final Environment environment;

	@Override
	public void send(String canonicalPhone, String message) {
		if (!environment.acceptsProfiles(Profiles.of("development", "test"))) {
			throw new IllegalStateException("SMS_PROVIDER=" + properties.provider()
					+ " logs codes and is for development only. Configure a real SMS provider.");
		}
		log.warn("[DEV SMS - not sent] to {}: {}", canonicalPhone, message);
	}
}
