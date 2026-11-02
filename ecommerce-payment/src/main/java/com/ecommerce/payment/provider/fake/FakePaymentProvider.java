package com.ecommerce.payment.provider.fake;

import com.ecommerce.payment.api.PaymentMethod;
import com.ecommerce.payment.spi.PaymentProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Development only: lets the storefront exercise online payments with no vendor account.
 * The "payment page" is our own callback URL with outcome=success; change it to fail or
 * cancel to test those paths. It trusts the browser by design, so it reports itself
 * unconfigured outside development/test, and routing refuses to start if a route points at it.
 */
@Component
@RequiredArgsConstructor
public class FakePaymentProvider implements PaymentProvider {

	public static final String ID = "fake";

	private final Environment environment;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public Set<PaymentMethod> supportedMethods() {
		return Set.of(PaymentMethod.BKASH, PaymentMethod.NAGAD, PaymentMethod.CARD);
	}

	@Override
	public boolean isConfigured() {
		return environment.acceptsProfiles(Profiles.of("development", "test"));
	}

	@Override
	public Initiation initiate(InitiationRequest r) {
		ensureDev();
		return new Initiation(r.callbackUrl() + "?ref=" + r.reference() + "&outcome=success", "fake-" + r.reference());
	}

	@Override
	public CallbackKey keyOf(Map<String, String> params) {
		return new CallbackKey(params.get("ref"), null);
	}

	@Override
	public Verification verify(PaymentRef payment, Map<String, String> params) {
		ensureDev();
		return switch (params.getOrDefault("outcome", "")) {
			case "success" -> new Verification(Outcome.PAID, "fake-trx-" + payment.reference(), payment.amount(), payment.currency(), "fake success");
			case "cancel" -> new Verification(Outcome.CANCELLED, null, null, null, "fake cancel");
			default -> new Verification(Outcome.FAILED, null, null, null, "fake failure");
		};
	}

	@Override
	public Verification query(PaymentRef payment) {
		return Verification.pending("fake provider keeps no state");
	}

	@Override
	public Refund refund(PaymentRef payment, String reason) {
		ensureDev();
		return new Refund(true, "fake-refund-" + payment.reference(), "fake refund");
	}

	private void ensureDev() {
		if (!isConfigured()) {
			throw new IllegalStateException("The fake payment provider is for development only");
		}
	}
}
