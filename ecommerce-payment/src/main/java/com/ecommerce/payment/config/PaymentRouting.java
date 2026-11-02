package com.ecommerce.payment.config;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.payment.api.PaymentMethod;
import com.ecommerce.payment.spi.PaymentProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Maps each enabled online method to its provider, from configuration only. Checked once at
 * startup: an enabled method whose provider is missing, does not support it or has no
 * credentials stops the application with a message naming the property to fix.
 */
@Slf4j
@Component
public class PaymentRouting {

	private final Map<String, PaymentProvider> providersById;
	private final Map<PaymentMethod, PaymentProvider> routes = new EnumMap<>(PaymentMethod.class);
	private final List<PaymentMethod> available;

	/** Hand-written: it validates the configuration and fails startup when it is wrong. */
	public PaymentRouting(PaymentProperties properties, List<PaymentProvider> providers) {
		this.providersById = providers.stream().collect(Collectors.toMap(PaymentProvider::id, Function.identity()));
		List<PaymentMethod> enabled = new ArrayList<>();
		for (PaymentMethod method : PaymentMethod.values()) {
			if (!properties.enabledMethods().contains(method)) {
				continue;
			}
			if (method.isOnline()) {
				String id = properties.routes().get(method);
				PaymentProvider provider = id == null ? null : providersById.get(id);
				if (provider == null) {
					throw new IllegalStateException("Payment method " + method + " is enabled but app.payment.routes."
							+ method + " names no known provider (have: " + providersById.keySet() + ")");
				}
				if (!provider.supportedMethods().contains(method)) {
					throw new IllegalStateException("Provider '" + id + "' does not support " + method
							+ " (supports " + provider.supportedMethods() + "); fix app.payment.routes." + method);
				}
				if (!provider.isConfigured()) {
					throw new IllegalStateException("Payment method " + method + " is enabled but provider '" + id
							+ "' has no credentials; set its environment variables (see .env.example)");
				}
				routes.put(method, provider);
				log.info("Payment method {} -> provider {}", method, id);
			}
			enabled.add(method);
		}
		this.available = List.copyOf(enabled);
	}

	public List<PaymentMethod> available() {
		return available;
	}

	public boolean isAvailable(PaymentMethod method) {
		return available.contains(method);
	}

	public PaymentProvider providerFor(PaymentMethod method) {
		PaymentProvider provider = routes.get(method);
		if (provider == null) {
			throw ResponseException.badRequest(ErrorCode.PAYMENT_METHOD_UNAVAILABLE);
		}
		return provider;
	}

	/** By id, for callbacks and for payments started before a route was switched. */
	public Optional<PaymentProvider> byId(String id) {
		return Optional.ofNullable(providersById.get(id));
	}
}
