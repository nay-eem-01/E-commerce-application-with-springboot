package com.ecommerce.payment.config;

import com.ecommerce.payment.api.PaymentMethod;
import com.ecommerce.payment.spi.PaymentProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentRoutingTest {

	private static PaymentProvider provider(String id, boolean configured, PaymentMethod... methods) {
		PaymentProvider p = mock(PaymentProvider.class);
		when(p.id()).thenReturn(id);
		when(p.isConfigured()).thenReturn(configured);
		when(p.supportedMethods()).thenReturn(Set.of(methods));
		return p;
	}

	private static PaymentProperties props(Set<PaymentMethod> enabled, Map<PaymentMethod, String> routes) {
		return new PaymentProperties(enabled, routes, "http://api", "http://shop/result", Duration.ofMinutes(30));
	}

	private final PaymentProvider bkash = provider("bkash", true, PaymentMethod.BKASH);
	private final PaymentProvider ssl = provider("sslcommerz", true, PaymentMethod.CARD, PaymentMethod.BKASH, PaymentMethod.NAGAD);

	@Test
	void switchingTheVendorIsOnlyAPropertyChange() {
		var direct = new PaymentRouting(props(Set.of(PaymentMethod.COD, PaymentMethod.BKASH), Map.of(PaymentMethod.BKASH, "bkash")), List.of(bkash, ssl));
		assertThat(direct.providerFor(PaymentMethod.BKASH)).isSameAs(bkash);

		var aggregator = new PaymentRouting(props(Set.of(PaymentMethod.COD, PaymentMethod.BKASH), Map.of(PaymentMethod.BKASH, "sslcommerz")), List.of(bkash, ssl));
		assertThat(aggregator.providerFor(PaymentMethod.BKASH)).isSameAs(ssl);
		assertThat(aggregator.available()).containsExactly(PaymentMethod.COD, PaymentMethod.BKASH);
	}

	@Test
	void codNeedsNoProviderAndDisabledMethodsAreNotOffered() {
		var routing = new PaymentRouting(props(Set.of(PaymentMethod.COD), Map.of(PaymentMethod.BKASH, "bkash")), List.of(bkash));
		assertThat(routing.available()).containsExactly(PaymentMethod.COD);
		assertThat(routing.isAvailable(PaymentMethod.BKASH)).isFalse();
	}

	@Test
	void startupFailsOnAMisconfiguredEnabledMethod() {
		PaymentProvider noCredentials = provider("bkash", false, PaymentMethod.BKASH);
		assertThatThrownBy(() -> new PaymentRouting(props(Set.of(PaymentMethod.BKASH), Map.of(PaymentMethod.BKASH, "bkash")), List.of(noCredentials)))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("no credentials");
		assertThatThrownBy(() -> new PaymentRouting(props(Set.of(PaymentMethod.CARD), Map.of(PaymentMethod.CARD, "bkash")), List.of(bkash)))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("does not support");
		assertThatThrownBy(() -> new PaymentRouting(props(Set.of(PaymentMethod.CARD), Map.of(PaymentMethod.CARD, "paypal")), List.of(bkash)))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("app.payment.routes.CARD");
	}
}
