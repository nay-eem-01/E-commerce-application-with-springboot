package com.ecommerce.user.service;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.logging.LogMasks;
import com.ecommerce.user.config.OtpProperties;
import com.ecommerce.user.dto.response.OtpSentResponse;
import com.ecommerce.user.entity.OtpVerification;
import com.ecommerce.user.enums.OtpPurpose;
import com.ecommerce.user.repository.OtpVerificationRepository;
import com.ecommerce.user.security.TokenHasher;
import com.ecommerce.user.sms.SmsSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Issues and checks sign-in codes. A code is 6 random digits, BCrypt-hashed, valid for
 * {@code app.otp.ttl}, and dies after {@code maxAttempts} wrong tries or one right one.
 * Every failure answers the same message so a caller cannot tell why it failed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

	private final OtpVerificationRepository otpRepository;
	private final PasswordEncoder passwordEncoder;
	private final SmsSender smsSender;
	private final OtpProperties properties;
	private final Clock clock;

	@Transactional
	public OtpSentResponse issue(String phone, OtpPurpose purpose) {
		Instant now = clock.instant();
		otpRepository.findFirstByPhoneAndPurposeAndConsumedAtIsNullOrderByIdDesc(phone, purpose)
				.filter(open -> open.getCreationDate() != null
						&& open.getCreationDate().plus(properties.resendCooldown()).isAfter(now))
				.ifPresent(open -> { throw ResponseException.tooManyRequests(); });

		otpRepository.consumeAllOpen(phone, purpose, now);

		String code = TokenHasher.newOtpCode();
		OtpVerification otp = new OtpVerification();
		otp.setPhone(phone);
		otp.setPurpose(purpose);
		otp.setCodeHash(passwordEncoder.encode(code));
		otp.setExpiresAt(now.plus(properties.ttl()));
		otpRepository.save(otp);

		smsSender.send(phone, code + " is your Senpai Supply sign-in code. It expires in "
				+ properties.ttl().toMinutes() + " minutes. Never share it.");
		log.info("Sign-in code issued for {}", LogMasks.maskPhone(phone));
		return new OtpSentResponse(properties.ttl().toSeconds(), properties.resendCooldown().toSeconds());
	}

	/** Consumes the code on success; throws 400 otherwise. Wrong tries are committed. */
	@Transactional(noRollbackFor = ResponseException.class)
	public void verify(String phone, OtpPurpose purpose, String code) {
		Instant now = clock.instant();
		OtpVerification otp = otpRepository.findFirstByPhoneAndPurposeAndConsumedAtIsNullOrderByIdDesc(phone, purpose)
				.orElseThrow(() -> ResponseException.badRequest(ErrorCode.INVALID_OTP));

		if (otp.getExpiresAt().isBefore(now) || otp.getAttempts() >= properties.maxAttempts()) {
			otp.setConsumedAt(now);
			throw ResponseException.badRequest(ErrorCode.INVALID_OTP);
		}
		if (!passwordEncoder.matches(code, otp.getCodeHash())) {
			otp.setAttempts(otp.getAttempts() + 1);
			if (otp.getAttempts() >= properties.maxAttempts()) {
				otp.setConsumedAt(now);
			}
			throw ResponseException.badRequest(ErrorCode.INVALID_OTP);
		}
		otp.setConsumedAt(now);
	}

	public Duration ttl() {
		return properties.ttl();
	}
}
