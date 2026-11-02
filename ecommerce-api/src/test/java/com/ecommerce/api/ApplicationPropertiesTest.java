package com.ecommerce.api;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards settings whose wrong value only shows up in a real deployment. */
class ApplicationPropertiesTest {

	private static Properties load() throws Exception {
		Properties p = new Properties();
		try (InputStream in = ApplicationPropertiesTest.class.getResourceAsStream("/application.properties")) {
			p.load(in);
		}
		return p;
	}

	/**
	 * Regression: smtp.auth was hard-coded to true, so a relay without credentials failed every
	 * send with MailAuthenticationException (found by sending through a real SMTP server).
	 */
	@Test
	void smtpAuthAndStarttlsAreEnvironmentSettings() throws Exception {
		Properties p = load();
		assertThat(p.getProperty("spring.mail.properties.mail.smtp.auth")).isEqualTo("${MAIL_SMTP_AUTH:true}");
		assertThat(p.getProperty("spring.mail.properties.mail.smtp.starttls.enable")).isEqualTo("${MAIL_SMTP_STARTTLS:true}");
	}

	@Test
	void secretsHaveNoDefaults() throws Exception {
		Properties p = load();
		assertThat(p.getProperty("app.jwt.secret")).isEqualTo("${JWT_SECRET}");
		assertThat(p.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
		assertThat(p.getProperty("spring.mail.password")).isEqualTo("${MAIL_PASSWORD:}");
		assertThat(p.getProperty("app.payment.bkash.app-secret")).isEqualTo("${BKASH_APP_SECRET:}");
		assertThat(p.getProperty("app.payment.sslcommerz.store-password")).isEqualTo("${SSLCOMMERZ_STORE_PASSWORD:}");
	}
}
