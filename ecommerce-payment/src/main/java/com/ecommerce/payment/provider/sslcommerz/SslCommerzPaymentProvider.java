package com.ecommerce.payment.provider.sslcommerz;

import com.ecommerce.common.money.Money;
import com.ecommerce.common.phone.PhoneNumbers;
import com.ecommerce.payment.api.PaymentMethod;
import com.ecommerce.payment.config.PaymentHttpConfig;
import com.ecommerce.payment.spi.PaymentProvider;
import com.ecommerce.payment.spi.ProviderException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SSLCommerz (v4): one integration for cards, internet banking and mobile wallets, so it can
 * serve CARD, and also BKASH or NAGAD if those routes are pointed here instead of a direct
 * integration. Flow: create session -> shopper pays on the gateway page -> the gateway posts
 * the shopper back (success/fail/cancel) and calls our IPN -> we confirm with the Order
 * Validation API (val_id) or the transaction query API (tran_id) and check tran_id, amount
 * and currency. Posted fields alone are never trusted.
 */
@Component
public class SslCommerzPaymentProvider implements PaymentProvider {

	public static final String ID = "sslcommerz";
	private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {};
	private static final Set<String> VALID = Set.of("VALID", "VALIDATED");

	private final SslCommerzProperties props;
	private final RestClient http;

	/** Hand-written: builds the vendor client from the shared, timeout-configured builder. */
	public SslCommerzPaymentProvider(SslCommerzProperties props, @Qualifier(PaymentHttpConfig.CLIENT) RestClient.Builder builder) {
		this.props = props;
		this.http = builder.clone().baseUrl(props.baseUrl() == null ? "http://sslcommerz.not-configured" : props.baseUrl()).build();
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public Set<PaymentMethod> supportedMethods() {
		return Set.of(PaymentMethod.CARD, PaymentMethod.BKASH, PaymentMethod.NAGAD);
	}

	@Override
	public boolean isConfigured() {
		return props.complete();
	}

	@Override
	public Initiation initiate(InitiationRequest r) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("store_id", props.storeId());
		form.add("store_passwd", props.storePassword());
		form.add("total_amount", Money.of(r.amount()).toPlainString());
		form.add("currency", "BDT");
		form.add("tran_id", r.reference());
		form.add("success_url", r.callbackUrl());
		form.add("fail_url", r.callbackUrl());
		form.add("cancel_url", r.callbackUrl());
		form.add("ipn_url", r.ipnUrl());
		form.add("cus_name", r.customerName());
		form.add("cus_email", r.customerEmail() != null ? r.customerEmail()
				: (props.fallbackEmail() != null ? props.fallbackEmail() : "orders@example.com"));
		form.add("cus_add1", r.addressLine());
		form.add("cus_city", r.district());
		form.add("cus_country", "Bangladesh");
		form.add("cus_phone", PhoneNumbers.toLocal(r.customerPhone()));
		form.add("shipping_method", "NO");
		form.add("num_of_item", String.valueOf(r.itemCount()));
		form.add("product_name", "Order " + r.orderNumber());
		form.add("product_category", "Anime merchandise");
		form.add("product_profile", "physical-goods");
		// Preselect the wallet when this route serves a wallet method.
		if (r.method() == PaymentMethod.BKASH) {
			form.add("multi_card_name", "bkash");
		} else if (r.method() == PaymentMethod.NAGAD) {
			form.add("multi_card_name", "nagad");
		}

		Map<String, Object> res;
		try {
			res = http.post().uri("/gwprocess/v4/api.php")
					.contentType(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_JSON)
					.body(form).retrieve().body(JSON);
		} catch (RestClientException e) {
			throw new ProviderException("SSLCommerz session failed: " + e.getClass().getSimpleName(), e);
		}
		if (res == null || !"SUCCESS".equalsIgnoreCase(String.valueOf(res.get("status"))) || res.get("GatewayPageURL") == null) {
			throw new ProviderException("SSLCommerz session refused: " + (res == null ? "no body" : res.get("failedreason")));
		}
		return new Initiation(res.get("GatewayPageURL").toString(), String.valueOf(res.get("sessionkey")));
	}

	@Override
	public CallbackKey keyOf(Map<String, String> params) {
		return new CallbackKey(params.get("tran_id"), null);
	}

	@Override
	public Verification verify(PaymentRef payment, Map<String, String> params) {
		String valId = params.get("val_id");
		if (valId == null || valId.isBlank()) {
			return query(payment);
		}
		Map<String, Object> res = get("/validator/api/validationserverAPI.php", Map.of("val_id", valId));
		String status = String.valueOf(res.get("status"));
		if (!VALID.contains(status)) {
			return new Verification(Outcome.FAILED, null, null, null, "SSLCommerz validation " + status);
		}
		if (!payment.reference().equals(res.get("tran_id"))) {
			return new Verification(Outcome.FAILED, null, null, null, "SSLCommerz tran_id does not match");
		}
		return paid(res);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Verification query(PaymentRef payment) {
		Map<String, Object> res = get("/validator/api/merchantTransIDvalidationAPI.php", Map.of("tran_id", payment.reference()));
		Object element = res.get("element");
		if (!(element instanceof List<?> list) || list.isEmpty()) {
			return Verification.pending("SSLCommerz has no transaction yet");
		}
		Outcome worst = Outcome.PENDING;
		for (Object o : list) {
			Map<String, Object> t = (Map<String, Object>) o;
			String status = String.valueOf(t.get("status"));
			if (VALID.contains(status) && payment.reference().equals(t.get("tran_id"))) {
				return paid(t);
			}
			if ("FAILED".equals(status)) {
				worst = Outcome.FAILED;
			} else if ("CANCELLED".equals(status) && worst == Outcome.PENDING) {
				worst = Outcome.CANCELLED;
			}
		}
		return new Verification(worst, null, null, null, "SSLCommerz query " + worst);
	}

	@Override
	public Refund refund(PaymentRef payment, String reason) {
		Map<String, Object> res = get("/validator/api/merchantTransIDvalidationAPI.php", Map.of(
				"bank_tran_id", payment.providerTransactionId(),
				"refund_amount", Money.of(payment.amount()).toPlainString(),
				"refund_remarks", reason.length() > 100 ? reason.substring(0, 100) : reason));
		boolean ok = "success".equalsIgnoreCase(String.valueOf(res.get("status")));
		return new Refund(ok, String.valueOf(res.get("refund_ref_id")), "SSLCommerz refund " + res.get("status") + " " + res.get("errorReason"));
	}

	/** Uses the order currency/amount SSLCommerz echoes back when present, else the settled ones. */
	private Verification paid(Map<String, Object> t) {
		Object amount = t.get("currency_amount") != null ? t.get("currency_amount") : t.get("amount");
		Object currency = t.get("currency_type") != null ? t.get("currency_type") : t.get("currency");
		return new Verification(Outcome.PAID, String.valueOf(t.get("bank_tran_id")),
				new BigDecimal(String.valueOf(amount)), currency == null ? null : currency.toString(), "SSLCommerz " + t.get("status"));
	}

	private Map<String, Object> get(String path, Map<String, String> params) {
		try {
			Map<String, Object> res = http.get().uri(b -> {
						b.path(path).queryParam("store_id", props.storeId()).queryParam("store_passwd", props.storePassword())
								.queryParam("format", "json").queryParam("v", "1");
						params.forEach(b::queryParam);
						return b.build();
					}).accept(MediaType.APPLICATION_JSON)
					.retrieve().body(JSON);
			if (res == null) {
				throw new ProviderException("SSLCommerz " + path + " returned no body");
			}
			return res;
		} catch (RestClientException e) {
			throw new ProviderException("SSLCommerz " + path + " failed: " + e.getClass().getSimpleName(), e);
		}
	}
}
