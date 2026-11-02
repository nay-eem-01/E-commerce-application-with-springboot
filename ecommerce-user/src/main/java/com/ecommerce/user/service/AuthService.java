package com.ecommerce.user.service;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.logging.LogMasks;
import com.ecommerce.common.phone.PhoneNumbers;
import com.ecommerce.common.ratelimit.RateLimiter;
import com.ecommerce.user.dto.request.OtpVerifyRequest;
import com.ecommerce.user.dto.request.SignInRequest;
import com.ecommerce.user.dto.response.OtpSentResponse;
import com.ecommerce.user.dto.response.TokenResponse;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.enums.OtpPurpose;
import com.ecommerce.user.enums.RoleType;
import com.ecommerce.user.enums.UserStatus;
import com.ecommerce.user.repository.RoleRepository;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

/**
 * Shoppers sign in with a phone code; the same flow creates the account on first use, so
 * there is no separate sign-up and no password to forget. Staff sign in with email and
 * password. Throttled per number/email and per IP; limits count unknown accounts too.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private static final Duration WINDOW_15M = Duration.ofMinutes(15);
	private static final Duration WINDOW_1H = Duration.ofHours(1);

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final OtpService otpService;
	private final SessionService sessionService;
	private final PasswordEncoder passwordEncoder;
	private final RateLimiter rateLimiter;
	private final Clock clock;

	public OtpSentResponse requestOtp(String rawPhone, String ip) {
		String phone = normalize(rawPhone);
		rateLimiter.hit("otp-send:ip:" + ip, 20, WINDOW_1H);
		rateLimiter.hit("otp-send:phone:" + phone, 5, WINDOW_1H);
		return otpService.issue(phone, OtpPurpose.SIGN_IN);
	}

	@Transactional(noRollbackFor = ResponseException.class)
	public TokenResponse verifyOtp(OtpVerifyRequest request, String ip, String userAgent) {
		String phone = normalize(request.getPhone());
		rateLimiter.hit("otp-verify:ip:" + ip, 30, WINDOW_15M);
		rateLimiter.hit("otp-verify:phone:" + phone, 10, WINDOW_15M);

		otpService.verify(phone, OtpPurpose.SIGN_IN, request.getCode());

		User user = userRepository.findByPhone(phone).orElseGet(() -> createCustomer(phone, request.getName()));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw ResponseException.forbidden(ErrorCode.ACCOUNT_BANNED);
		}
		if (user.getPhoneVerifiedAt() == null) {
			user.setPhoneVerifiedAt(clock.instant());
		}
		rateLimiter.reset("otp-verify:phone:" + phone);
		return sessionService.openSession(user, request.getDeviceLabel() != null ? request.getDeviceLabel() : userAgent);
	}

	/** Verify credentials first, then status. Every credential failure is the same 401. */
	@Transactional
	public TokenResponse signIn(SignInRequest request, String ip, String userAgent) {
		String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
		String emailKey = "signin:email:" + email;
		String ipKey = "signin:ip:" + ip;
		if (rateLimiter.isBlocked(emailKey) || rateLimiter.isBlocked(ipKey)) {
			throw ResponseException.tooManyRequests();
		}

		User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
		boolean ok = user != null && user.getPasswordHash() != null
				&& passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
		if (!ok) {
			rateLimiter.record(emailKey, 5, WINDOW_15M);
			rateLimiter.record(ipKey, 20, WINDOW_15M);
			log.warn("Failed password sign-in for {}", LogMasks.maskEmail(email));
			throw ResponseException.unauthorized(ErrorCode.INVALID_CREDENTIALS);
		}
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw ResponseException.forbidden(ErrorCode.ACCOUNT_BANNED);
		}
		rateLimiter.reset(emailKey);
		return sessionService.openSession(user, userAgent);
	}

	private User createCustomer(String phone, String name) {
		Role customer = roleRepository.findByName(RoleType.CUSTOMER)
				.orElseThrow(() -> new IllegalStateException("CUSTOMER role missing; Flyway seed did not run"));
		User user = new User();
		user.setPhone(phone);
		user.setName(name == null || name.isBlank() ? null : name.trim());
		user.getRoles().add(customer);
		log.info("New customer account for {}", LogMasks.maskPhone(phone));
		return userRepository.save(user);
	}

	private String normalize(String raw) {
		return PhoneNumbers.normalize(raw).orElseThrow(() -> ResponseException.badRequest(ErrorCode.INVALID_PHONE));
	}
}
