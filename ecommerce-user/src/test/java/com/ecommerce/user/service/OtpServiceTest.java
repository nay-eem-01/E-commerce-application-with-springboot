package com.ecommerce.user.service;

import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.user.config.OtpProperties;
import com.ecommerce.user.entity.OtpVerification;
import com.ecommerce.user.enums.OtpPurpose;
import com.ecommerce.user.repository.OtpVerificationRepository;
import com.ecommerce.user.sms.SmsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtpServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");
	private static final String PHONE = "+8801712345678";

	private final OtpVerificationRepository repo = mock(OtpVerificationRepository.class);
	private final SmsSender sms = mock(SmsSender.class);
	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
	private OtpService service;
	private OtpVerification otp;

	@BeforeEach
	void setUp() {
		service = new OtpService(repo, encoder, sms, new OtpProperties(Duration.ofMinutes(5), 3, Duration.ofSeconds(60)),
				Clock.fixed(NOW, ZoneOffset.UTC));
		otp = new OtpVerification();
		otp.setPhone(PHONE);
		otp.setPurpose(OtpPurpose.SIGN_IN);
		otp.setCodeHash(encoder.encode("123456"));
		otp.setExpiresAt(NOW.plus(Duration.ofMinutes(5)));
		when(repo.findFirstByPhoneAndPurposeAndConsumedAtIsNullOrderByIdDesc(PHONE, OtpPurpose.SIGN_IN)).thenReturn(Optional.of(otp));
	}

	@Test
	void theRightCodeIsConsumedOnce() {
		service.verify(PHONE, OtpPurpose.SIGN_IN, "123456");
		assertThat(otp.getConsumedAt()).isEqualTo(NOW);
	}

	@Test
	void wrongCodesCountAndTheCodeDiesAtTheLimit() {
		for (int i = 1; i <= 3; i++) {
			assertThatThrownBy(() -> service.verify(PHONE, OtpPurpose.SIGN_IN, "000000")).isInstanceOf(ResponseException.class);
		}
		assertThat(otp.getAttempts()).isEqualTo(3);
		assertThat(otp.getConsumedAt()).isNotNull();
	}

	@Test
	void anExpiredCodeIsRejectedEvenIfCorrect() {
		otp.setExpiresAt(NOW.minusSeconds(1));
		assertThatThrownBy(() -> service.verify(PHONE, OtpPurpose.SIGN_IN, "123456")).isInstanceOf(ResponseException.class);
	}

	@Test
	void issuingStoresOnlyAHashAndRespectsTheResendCooldown() {
		otp.setCreationDate(NOW.minusSeconds(10));
		assertThatThrownBy(() -> service.issue(PHONE, OtpPurpose.SIGN_IN)).isInstanceOf(ResponseException.class);

		otp.setCreationDate(NOW.minusSeconds(120));
		service.issue(PHONE, OtpPurpose.SIGN_IN);
		ArgumentCaptor<OtpVerification> saved = ArgumentCaptor.forClass(OtpVerification.class);
		verify(repo).save(saved.capture());
		ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
		verify(sms).send(eq(PHONE), message.capture());
		String code = message.getValue().substring(0, 6);
		assertThat(saved.getValue().getCodeHash()).doesNotContain(code);
		assertThat(encoder.matches(code, saved.getValue().getCodeHash())).isTrue();
		verify(repo).consumeAllOpen(eq(PHONE), eq(OtpPurpose.SIGN_IN), eq(NOW));
	}
}
