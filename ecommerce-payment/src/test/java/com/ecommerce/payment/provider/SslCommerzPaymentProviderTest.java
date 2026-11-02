package com.ecommerce.payment.provider;

import com.ecommerce.payment.api.PaymentMethod;
import com.ecommerce.payment.provider.sslcommerz.SslCommerzPaymentProvider;
import com.ecommerce.payment.provider.sslcommerz.SslCommerzProperties;
import com.ecommerce.payment.spi.PaymentProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SslCommerzPaymentProviderTest {

	private static final String BASE = "https://sandbox.sslcommerz.com";
	private MockRestServiceServer server;
	private SslCommerzPaymentProvider provider;
	private final PaymentProvider.PaymentRef ref = new PaymentProvider.PaymentRef("PAY-ABC", "sess", null, new BigDecimal("1420.00"), "BDT");

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		provider = new SslCommerzPaymentProvider(new SslCommerzProperties(BASE, "store1", "secret", "orders@shop.test"), builder);
	}

	@Test
	void initiateOpensASessionWithOurReferenceAndAmount() {
		server.expect(requestTo(BASE + "/gwprocess/v4/api.php")).andExpect(method(HttpMethod.POST))
				.andExpect(content().formDataContains(Map.of("tran_id", "PAY-ABC", "total_amount", "1420.00", "currency", "BDT",
						"cus_email", "orders@shop.test", "multi_card_name", "bkash")))
				.andRespond(withSuccess("{\"status\":\"SUCCESS\",\"GatewayPageURL\":\"https://sandbox.sslcommerz.com/pay/1\",\"sessionkey\":\"sess\"}", MediaType.APPLICATION_JSON));
		var r = provider.initiate(new PaymentProvider.InitiationRequest("PAY-ABC", "SS-1", PaymentMethod.BKASH, new BigDecimal("1420"),
				"Rafi", "+8801712345678", null, "Dhaka", "House 1", 2, "https://api/cb", "https://api/ipn"));
		assertThat(r.redirectUrl()).isEqualTo("https://sandbox.sslcommerz.com/pay/1");
		server.verify();
	}

	@Test
	void anIpnIsConfirmedWithTheValidationApi() {
		server.expect(requestTo(startsWith(BASE + "/validator/api/validationserverAPI.php")))
				.andExpect(queryParam("val_id", "V1")).andExpect(queryParam("store_id", "store1"))
				.andRespond(withSuccess("{\"status\":\"VALID\",\"tran_id\":\"PAY-ABC\",\"amount\":\"1420.00\",\"currency\":\"BDT\",\"bank_tran_id\":\"B1\"}", MediaType.APPLICATION_JSON));
		var v = provider.verify(ref, Map.of("tran_id", "PAY-ABC", "val_id", "V1", "status", "VALID"));
		assertThat(v.outcome()).isEqualTo(PaymentProvider.Outcome.PAID);
		assertThat(v.providerTransactionId()).isEqualTo("B1");
	}

	@Test
	void aValidationForAnotherTransactionIsRejected() {
		server.expect(requestTo(startsWith(BASE + "/validator/api/validationserverAPI.php")))
				.andRespond(withSuccess("{\"status\":\"VALID\",\"tran_id\":\"PAY-OTHER\",\"amount\":\"1420.00\"}", MediaType.APPLICATION_JSON));
		var v = provider.verify(ref, Map.of("tran_id", "PAY-ABC", "val_id", "V-of-another-order"));
		assertThat(v.outcome()).isEqualTo(PaymentProvider.Outcome.FAILED);
	}

	@Test
	void withoutAValIdTheTransactionQueryDecides() {
		server.expect(requestTo(startsWith(BASE + "/validator/api/merchantTransIDvalidationAPI.php"))).andExpect(queryParam("tran_id", "PAY-ABC"))
				.andRespond(withSuccess("{\"APIConnect\":\"DONE\",\"element\":[{\"status\":\"CANCELLED\",\"tran_id\":\"PAY-ABC\"}]}", MediaType.APPLICATION_JSON));
		assertThat(provider.verify(ref, Map.of("tran_id", "PAY-ABC", "status", "VALID")).outcome())
				.as("a posted status=VALID without val_id is not believed").isEqualTo(PaymentProvider.Outcome.CANCELLED);
	}
}
