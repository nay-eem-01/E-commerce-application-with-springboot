package com.ecommerce.payment.provider.bkash;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * bKash tokenized checkout credentials (merchant portal). All from the environment, no
 * defaults. baseUrl switches sandbox (https://tokenized.sandbox.bka.sh/v1.2.0-beta) and live.
 */
@ConfigurationProperties(prefix = "app.payment.bkash")
public record BkashProperties(String baseUrl, String appKey, String appSecret, String username, String password) {

	public boolean complete() {
		return notBlank(baseUrl) && notBlank(appKey) && notBlank(appSecret) && notBlank(username) && notBlank(password);
	}

	private static boolean notBlank(String s) {
		return s != null && !s.isBlank();
	}
}
