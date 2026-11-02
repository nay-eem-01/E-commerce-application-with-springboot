package com.ecommerce.payment.provider;

import com.ecommerce.payment.api.PaymentMethod;
import com.ecommerce.payment.provider.bkash.BkashPaymentProvider;
import com.ecommerce.payment.provider.bkash.BkashProperties;
import com.ecommerce.payment.spi.PaymentProvider;
import com.ecommerce.payment.spi.ProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BkashPaymentProviderTest {

	private static final String BASE = "https://tokenized.sandbox.bka.sh/v1.2.0-beta";
	private MockRestServiceServer server;
	private BkashPaymentProvider provider;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		provider = new BkashPaymentProvider(new BkashProperties(BASE, "app-key", "app-secret", "user", "pass"), builder, Clock.systemUTC());
	}

	private void expectGrant() {
		server.expect(requestTo(BASE + "/tokenized/checkout/token/grant")).andExpect(method(HttpMethod.POST))
				.andExpect(header("username", "user")).andExpect(jsonPath("$.app_key").value("app-key"))
				.andRespond(withSuccess("{\"id_token\":\"tok-1\",\"expires_in\":3600,\"statusCode\":\"0000\"}", MediaType.APPLICATION_JSON));
	}

	private static PaymentProvider.InitiationRequest request() {
		return new PaymentProvider.InitiationRequest("PAY-ABC", "SS-1", PaymentMethod.BKASH, new BigDecimal("1420"), "Rafi",
				"+8801712345678", null, "Dhaka", "House 1", 1, "https://api/api/payments/bkash/callback", "https://api/api/payments/bkash/ipn");
	}

	@Test
	void initiateGrantsATokenThenCreatesThePayment() {
		expectGrant();
		server.expect(requestTo(BASE + "/tokenized/checkout/create"))
				.andExpect(header("Authorization", "tok-1")).andExpect(header("X-APP-Key", "app-key"))
				.andExpect(jsonPath("$.mode").value("0011")).andExpect(jsonPath("$.amount").value("1420.00"))
				.andExpect(jsonPath("$.merchantInvoiceNumber").value("PAY-ABC")).andExpect(jsonPath("$.payerReference").value("01712345678"))
				.andRespond(withSuccess("{\"statusCode\":\"0000\",\"paymentID\":\"TR001\",\"bkashURL\":\"https://sandbox.bka.sh/pay/TR001\"}", MediaType.APPLICATION_JSON));

		PaymentProvider.Initiation result = provider.initiate(request());
		assertThat(result.redirectUrl()).isEqualTo("https://sandbox.bka.sh/pay/TR001");
		assertThat(result.providerPaymentId()).isEqualTo("TR001");
		server.verify();
	}

	@Test
	void theTokenIsReusedUntilItExpires() {
		expectGrant();
		server.expect(requestTo(BASE + "/tokenized/checkout/payment/status"))
				.andRespond(withSuccess("{\"transactionStatus\":\"Initiated\"}", MediaType.APPLICATION_JSON));
		server.expect(requestTo(BASE + "/tokenized/checkout/payment/status")).andExpect(header("Authorization", "tok-1"))
				.andRespond(withSuccess("{\"transactionStatus\":\"Initiated\"}", MediaType.APPLICATION_JSON));
		var ref = new PaymentProvider.PaymentRef("PAY-ABC", "TR001", null, new BigDecimal("1420.00"), "BDT");
		provider.query(ref);
		provider.query(ref);
		server.verify();
	}

	@Test
	void aSuccessCallbackIsConfirmedByExecute() {
		expectGrant();
		server.expect(requestTo(BASE + "/tokenized/checkout/execute")).andExpect(jsonPath("$.paymentID").value("TR001"))
				.andRespond(withSuccess("{\"statusCode\":\"0000\",\"transactionStatus\":\"Completed\",\"trxID\":\"9XY\",\"amount\":\"1420.00\",\"currency\":\"BDT\"}", MediaType.APPLICATION_JSON));
		var v = provider.verify(new PaymentProvider.PaymentRef("PAY-ABC", "TR001", null, new BigDecimal("1420.00"), "BDT"),
				Map.of("paymentID", "TR001", "status", "success"));
		assertThat(v.outcome()).isEqualTo(PaymentProvider.Outcome.PAID);
		assertThat(v.providerTransactionId()).isEqualTo("9XY");
		assertThat(v.amount()).isEqualByComparingTo("1420.00");
	}

	@Test
	void aCancelCallbackIsCheckedWithTheStatusQueryNotTrusted() {
		expectGrant();
		server.expect(requestTo(BASE + "/tokenized/checkout/payment/status"))
				.andRespond(withSuccess("{\"transactionStatus\":\"Completed\",\"trxID\":\"9XY\",\"amount\":\"1420.00\"}", MediaType.APPLICATION_JSON));
		var v = provider.verify(new PaymentProvider.PaymentRef("PAY-ABC", "TR001", null, new BigDecimal("1420.00"), "BDT"),
				Map.of("paymentID", "TR001", "status", "cancel"));
		// The shopper said "cancel" but bKash says it was paid: the money wins.
		assertThat(v.outcome()).isEqualTo(PaymentProvider.Outcome.PAID);
	}

	@Test
	void vendorErrorsBecomeProviderException() {
		server.expect(requestTo(BASE + "/tokenized/checkout/token/grant")).andRespond(withServerError());
		assertThatThrownBy(() -> provider.initiate(request())).isInstanceOf(ProviderException.class).hasMessageNotContaining("pass");
	}
}
