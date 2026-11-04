package com.ecommerce.api;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards settings whose wrong value only shows up in a real deployment. */
class ApplicationPropertiesTest {

	private static Properties load() throws Exception {
		return load("/application.properties");
	}

	private static Properties load(String file) throws Exception {
		Properties p = new Properties();
		try (InputStream in = ApplicationPropertiesTest.class.getResourceAsStream(file)) {
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

	/**
	 * Regression: multipart was switched off globally, so the image upload endpoint answered 500 ("no multi-part
	 * configuration") in the running app while its unit tests passed. Parts are parsed lazily, after security.
	 */
	@Test
	void multipartIsOnForUploadsWithLimitsAndLazyParsing() throws Exception {
		Properties p = load();
		assertThat(p.getProperty("spring.servlet.multipart.enabled")).isEqualTo("true");
		assertThat(p.getProperty("spring.servlet.multipart.resolve-lazily")).isEqualTo("true");
		assertThat(p.getProperty("spring.servlet.multipart.max-file-size")).isEqualTo("6MB");
		assertThat(p.getProperty("spring.servlet.multipart.max-request-size")).isEqualTo("7MB");
	}

	@Test
	void secretsHaveNoDefaults() throws Exception {
		Properties p = load();
		assertThat(p.getProperty("app.jwt.secret")).isEqualTo("${JWT_SECRET}");
		assertThat(p.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
		assertThat(p.getProperty("spring.mail.password")).isEqualTo("${MAIL_PASSWORD:}");
		assertThat(p.getProperty("app.payment.bkash.app-secret")).isEqualTo("${BKASH_APP_SECRET:}");
		assertThat(p.getProperty("app.payment.sslcommerz.store-password")).isEqualTo("${SSLCOMMERZ_STORE_PASSWORD:}");
		assertThat(p.getProperty("app.storage.s3.secret-access-key")).isEqualTo("${S3_SECRET_ACCESS_KEY:}");
	}

	/**
	 * In production images must go to a bucket and be served from a real URL: without these the app would start with
	 * every image address pointing at localhost. Missing values fail startup, as APP_BASE_URL does.
	 */
	@Test
	void productionRequiresABucketAndAPublicImageUrl() throws Exception {
		Properties p = load("/application-production.properties");
		assertThat(p.getProperty("app.storage.s3.bucket")).isEqualTo("${S3_BUCKET}");
		assertThat(p.getProperty("app.storage.public-base-url")).isEqualTo("${UPLOADS_PUBLIC_BASE_URL}");
	}
}
