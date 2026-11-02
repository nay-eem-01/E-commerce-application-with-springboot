package com.ecommerce.payment.provider.sslcommerz;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SSLCommerz store credentials. baseUrl: https://sandbox.sslcommerz.com (sandbox) or
 * https://securepay.sslcommerz.com (live). fallbackEmail is used when a guest gave no email,
 * because SSLCommerz requires one.
 */
@ConfigurationProperties(prefix = "app.payment.sslcommerz")
public record SslCommerzProperties(String baseUrl, String storeId, String storePassword, String fallbackEmail) {

	public boolean complete() {
		return baseUrl != null && !baseUrl.isBlank() && storeId != null && !storeId.isBlank()
				&& storePassword != null && !storePassword.isBlank();
	}
}
