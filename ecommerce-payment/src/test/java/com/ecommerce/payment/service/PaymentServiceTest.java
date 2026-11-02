package com.ecommerce.payment.service;

import com.ecommerce.common.event.PaymentExpiredEvent;
import com.ecommerce.common.event.PaymentSucceededEvent;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.payment.api.OnlinePaymentRequest;
import com.ecommerce.payment.api.PaymentMethod;
import com.ecommerce.payment.api.PaymentStatus;
import com.ecommerce.payment.config.PaymentProperties;
import com.ecommerce.payment.config.PaymentRouting;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.repository.PaymentEventRepository;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.spi.PaymentProvider;
import com.ecommerce.payment.spi.PaymentProvider.Outcome;
import com.ecommerce.payment.spi.PaymentProvider.Verification;
import com.ecommerce.payment.spi.ProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");

	private final PaymentRepository payments = mock(PaymentRepository.class);
	private final PaymentProvider provider = mock(PaymentProvider.class);
	private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
	private PaymentService service;
	private Payment payment;

	@BeforeEach
	void setUp() {
		when(provider.id()).thenReturn("bkash");
		when(provider.isConfigured()).thenReturn(true);
		when(provider.supportedMethods()).thenReturn(Set.of(PaymentMethod.BKASH));
		PaymentProperties props = new PaymentProperties(Set.of(PaymentMethod.COD, PaymentMethod.BKASH),
				Map.of(PaymentMethod.BKASH, "bkash"), "https://api.shop", "https://shop/result", Duration.ofMinutes(30));
		service = new PaymentService(payments, mock(PaymentEventRepository.class), new PaymentRouting(props, List.of(provider)),
				props, events, Clock.fixed(NOW, ZoneOffset.UTC), mock(PlatformTransactionManager.class));

		payment = new Payment();
		payment.setId(5L);
		payment.setOrderId(9L);
		payment.setOrderNumber("SS-1");
		payment.setReference("PAY-1");
		payment.setMethod(PaymentMethod.BKASH);
		payment.setProvider("bkash");
		payment.setStatus(PaymentStatus.PENDING);
		payment.setAmount(new BigDecimal("1420.00"));
		payment.setProviderPaymentId("TR001");
		when(payments.findById(5L)).thenReturn(Optional.of(payment));
		when(payments.findByProviderAndProviderPaymentId("bkash", "TR001")).thenReturn(Optional.of(payment));
		when(provider.keyOf(any())).thenReturn(new PaymentProvider.CallbackKey(null, "TR001"));
	}

	private static Verification paid(String amount) {
		return new Verification(Outcome.PAID, "TRX", new BigDecimal(amount), "BDT", "ok");
	}

	@Test
	void aVerifiedCallbackMarksThePaymentPaidAndAnnouncesIt() {
		when(provider.verify(any(), any())).thenReturn(paid("1420.00"));
		var result = service.handleCallback("bkash", Map.of("paymentID", "TR001", "status", "success"), "CALLBACK");
		assertThat(result.status()).isEqualTo(PaymentStatus.PAID);
		assertThat(payment.getProviderTransactionId()).isEqualTo("TRX");
		verify(events).publishEvent(any(PaymentSucceededEvent.class));
	}

	@Test
	void aPaymentForTheWrongAmountIsNeverMarkedPaid() {
		when(provider.verify(any(), any())).thenReturn(paid("14.20"));
		var result = service.handleCallback("bkash", Map.of("paymentID", "TR001"), "IPN");
		assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
		verify(events, never()).publishEvent(any(PaymentSucceededEvent.class));
	}

	@Test
	void repeatedCallbacksForAPaidPaymentDoNothing() {
		payment.setStatus(PaymentStatus.PAID);
		var result = service.handleCallback("bkash", Map.of("paymentID", "TR001"), "IPN");
		assertThat(result.status()).isEqualTo(PaymentStatus.PAID);
		verify(provider, never()).verify(any(), any());
	}

	@Test
	void aCallbackNamingAnotherProvidersPaymentIsNotFound() {
		payment.setProvider("sslcommerz");
		assertThatThrownBy(() -> service.handleCallback("bkash", Map.of("paymentID", "TR001"), "CALLBACK"))
				.extracting(e -> ((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void anUnreachableProviderLeavesThePaymentPending() {
		when(provider.verify(any(), any())).thenThrow(new ProviderException("timeout"));
		assertThat(service.handleCallback("bkash", Map.of("paymentID", "TR001"), "CALLBACK").status()).isEqualTo(PaymentStatus.PENDING);
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
	}

	@Test
	void moneyArrivingAfterExpiryIsRefundedAutomatically() {
		payment.setStatus(PaymentStatus.EXPIRED);
		when(provider.verify(any(), any())).thenReturn(paid("1420.00"));
		when(provider.refund(any(), anyString())).thenReturn(new PaymentProvider.Refund(true, "R1", "ok"));
		assertThat(service.handleCallback("bkash", Map.of("paymentID", "TR001"), "IPN").status()).isEqualTo(PaymentStatus.REFUNDED);
	}

	@Test
	void cancellingAPaidOnlineOrderRefundsAndAFailedRefundBlocksTheCancel() {
		payment.setStatus(PaymentStatus.PAID);
		when(payments.findByOrderIdOrderByIdDesc(9L)).thenReturn(List.of(payment));
		when(provider.refund(any(), anyString())).thenReturn(new PaymentProvider.Refund(false, null, "insufficient balance"));
		assertThatThrownBy(() -> service.settleCancellation(9L))
				.extracting(e -> ((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);

		when(provider.refund(any(), anyString())).thenReturn(new PaymentProvider.Refund(true, "R1", "ok"));
		service.settleCancellation(9L);
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
	}

	@Test
	void aProviderOutageAtStartFailsTheAttemptWith502() {
		when(provider.initiate(any())).thenThrow(new ProviderException("down"));
		when(payments.findByOrderIdOrderByIdDesc(9L)).thenReturn(List.of());
		when(payments.save(any())).thenAnswer(i -> { Payment p = i.getArgument(0); p.setId(5L); return p; });
		when(payments.findById(5L)).thenReturn(Optional.of(payment));
		assertThatThrownBy(() -> service.startOnline(new OnlinePaymentRequest(9L, "SS-1", PaymentMethod.BKASH, new BigDecimal("1420"),
				"Rafi", "+8801712345678", null, "Dhaka", "House 1", 1)))
				.extracting(e -> ((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
	}

	@Test
	void overduePaymentsAreCheckedOnceMoreThenExpired() {
		when(payments.findExpiring(any(), any(), any())).thenReturn(List.of(payment));
		when(provider.query(any())).thenReturn(Verification.pending("Initiated"));
		service.expireOverdue();
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
		verify(events).publishEvent(any(PaymentExpiredEvent.class));
	}

	@Test
	void anOverduePaymentThatWasActuallyPaidIsNotExpired() {
		when(payments.findExpiring(any(), any(), any())).thenReturn(List.of(payment));
		when(provider.query(any())).thenReturn(paid("1420.00"));
		service.expireOverdue();
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
		verify(events, never()).publishEvent(any(PaymentExpiredEvent.class));
	}
}
