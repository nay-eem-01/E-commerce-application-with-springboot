package com.ecommerce.payment.provider.bkash;

import com.ecommerce.common.money.Money;
import com.ecommerce.common.phone.PhoneNumbers;
import com.ecommerce.payment.api.PaymentMethod;
import com.ecommerce.payment.config.PaymentHttpConfig;
import com.ecommerce.payment.spi.PaymentProvider;
import com.ecommerce.payment.spi.ProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * bKash tokenized checkout (v1.2.0-beta): grant token -> create -> shopper approves in the
 * bKash page -> callback with paymentID and status -> execute (server to server) -> trxID.
 * The callback's "status=success" is only a hint; the payment counts once execute or the
 * status query says "Completed" with our amount.
 *
 * The id_token is cached until shortly before it expires (per instance).
 */
@Slf4j
@Component
public class BkashPaymentProvider implements PaymentProvider {

	public static final String ID = "bkash";
	private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {};
	private static final String OK = "0000";

	private final BkashProperties props;
	private final RestClient http;
	private final Clock clock;
	private String idToken;
	private Instant tokenExpiresAt = Instant.EPOCH;

	/** Hand-written: builds the vendor client from the shared, timeout-configured builder. */
	public BkashPaymentProvider(BkashProperties props, @Qualifier(PaymentHttpConfig.CLIENT) RestClient.Builder builder, Clock clock) {
		this.props = props;
		this.clock = clock;
		this.http = builder.clone().baseUrl(props.baseUrl() == null ? "http://bkash.not-configured" : props.baseUrl()).build();
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public Set<PaymentMethod> supportedMethods() {
		return Set.of(PaymentMethod.BKASH);
	}

	@Override
	public boolean isConfigured() {
		return props.complete();
	}

	@Override
	public Initiation initiate(InitiationRequest r) {
		Map<String, Object> body = Map.of(
				"mode", "0011",
				"payerReference", PhoneNumbers.toLocal(r.customerPhone()),
				"callbackURL", r.callbackUrl(),
				"amount", Money.of(r.amount()).toPlainString(),
				"currency", "BDT",
				"intent", "sale",
				"merchantInvoiceNumber", r.reference());
		Map<String, Object> res = post("/tokenized/checkout/create", body);
		Object url = res.get("bkashURL");
		Object paymentId = res.get("paymentID");
		if (!OK.equals(res.get("statusCode")) || url == null || paymentId == null) {
			throw new ProviderException("bKash create failed: statusCode=" + res.get("statusCode") + " " + res.get("statusMessage"));
		}
		return new Initiation(url.toString(), paymentId.toString());
	}

	@Override
	public CallbackKey keyOf(Map<String, String> params) {
		return new CallbackKey(null, params.get("paymentID"));
	}

	@Override
	public Verification verify(PaymentRef payment, Map<String, String> params) {
		String hint = params.getOrDefault("status", "");
		if (payment.providerPaymentId() == null) {
			return Verification.pending("no bKash paymentID stored yet");
		}
		if (!"success".equalsIgnoreCase(hint)) {
			Verification current = query(payment);
			if (current.outcome() == Outcome.PAID) {
				return current;
			}
			return new Verification("cancel".equalsIgnoreCase(hint) ? Outcome.CANCELLED : Outcome.FAILED,
					null, null, null, "shopper returned with status=" + hint);
		}
		Map<String, Object> res = post("/tokenized/checkout/execute", Map.of("paymentID", payment.providerPaymentId()));
		if (OK.equals(res.get("statusCode")) && "Completed".equals(res.get("transactionStatus"))) {
			return paid(res);
		}
		// Already executed, a timeout on our side, or a decline: the status query is the truth.
		return query(payment);
	}

	@Override
	public Verification query(PaymentRef payment) {
		if (payment.providerPaymentId() == null) {
			return Verification.pending("no bKash paymentID");
		}
		Map<String, Object> res = post("/tokenized/checkout/payment/status", Map.of("paymentID", payment.providerPaymentId()));
		Object status = res.get("transactionStatus");
		if ("Completed".equals(status)) {
			return paid(res);
		}
		if ("Failed".equals(status) || "Cancelled".equals(status) || "Expired".equals(status)) {
			return new Verification("Cancelled".equals(status) ? Outcome.CANCELLED : Outcome.FAILED, null, null, null, "bKash status " + status);
		}
		return Verification.pending("bKash status " + status);
	}

	@Override
	public Refund refund(PaymentRef payment, String reason) {
		Map<String, Object> res = post("/tokenized/checkout/payment/refund", Map.of(
				"paymentID", payment.providerPaymentId(),
				"trxID", payment.providerTransactionId(),
				"amount", Money.of(payment.amount()).toPlainString(),
				"sku", payment.reference(),
				"reason", reason.length() > 100 ? reason.substring(0, 100) : reason));
		boolean ok = "Completed".equals(res.get("transactionStatus"));
		return new Refund(ok, String.valueOf(res.get("refundTrxID")), "bKash " + res.get("statusCode") + " " + res.get("statusMessage"));
	}

	private Verification paid(Map<String, Object> res) {
		return new Verification(Outcome.PAID, String.valueOf(res.get("trxID")),
				new BigDecimal(String.valueOf(res.get("amount"))), String.valueOf(res.getOrDefault("currency", "BDT")), "bKash Completed");
	}

	private Map<String, Object> post(String path, Map<String, Object> body) {
		try {
			Map<String, Object> res = http.post().uri(path)
					.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
					.header("Authorization", token()).header("X-APP-Key", props.appKey())
					.body(body).retrieve().body(JSON);
			if (res == null) {
				throw new ProviderException("bKash " + path + " returned no body");
			}
			return res;
		} catch (RestClientException e) {
			throw new ProviderException("bKash " + path + " failed: " + e.getClass().getSimpleName(), e);
		}
	}

	/** Grants a token or reuses the cached one; refreshes a minute before expiry. */
	synchronized String token() {
		Instant now = clock.instant();
		if (idToken != null && now.isBefore(tokenExpiresAt)) {
			return idToken;
		}
		try {
			Map<String, Object> res = http.post().uri("/tokenized/checkout/token/grant")
					.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
					.header("username", props.username()).header("password", props.password())
					.body(Map.of("app_key", props.appKey(), "app_secret", props.appSecret()))
					.retrieve().body(JSON);
			if (res == null || res.get("id_token") == null) {
				throw new ProviderException("bKash token grant refused: " + (res == null ? "no body" : res.get("statusCode") + " " + res.get("statusMessage")));
			}
			long expiresIn = Long.parseLong(String.valueOf(res.getOrDefault("expires_in", "3600")));
			idToken = res.get("id_token").toString();
			tokenExpiresAt = now.plusSeconds(Math.max(60, expiresIn - 60));
			return idToken;
		} catch (RestClientException e) {
			throw new ProviderException("bKash token grant failed: " + e.getClass().getSimpleName(), e);
		}
	}
}
