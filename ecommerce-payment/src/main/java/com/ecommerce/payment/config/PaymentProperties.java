package com.ecommerce.payment.config;

import com.ecommerce.payment.api.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * enabledMethods: what checkout offers. routes: which provider processes each online method
 * (e.g. BKASH=bkash, CARD=sslcommerz). Switching vendor is editing a route; enabling a method
 * is adding it to enabledMethods once its provider has credentials.
 *
 * callbackBaseUrl: public base URL of this API (providers redirect and post to it).
 * returnUrl: storefront page the shopper lands on after paying.
 */
@Validated
@ConfigurationProperties(prefix = "app.payment")
public record PaymentProperties(
		@NotEmpty Set<PaymentMethod> enabledMethods,
		Map<PaymentMethod, String> routes,
		@NotNull String callbackBaseUrl,
		@NotNull String returnUrl,
		@NotNull Duration pendingTtl
) {
	public Map<PaymentMethod, String> routes() {
		return routes == null ? Map.of() : routes;
	}
}
