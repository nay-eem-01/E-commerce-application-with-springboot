package com.ecommerce.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** One HTTP client builder for vendor calls, with timeouts: a hung gateway must not hang checkout. */
@Configuration
public class PaymentHttpConfig {

	public static final String CLIENT = "paymentRestClientBuilder";

	@Bean(CLIENT)
	public RestClient.Builder paymentRestClientBuilder() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(5));
		factory.setReadTimeout(Duration.ofSeconds(20));
		return RestClient.builder().requestFactory(factory);
	}
}
