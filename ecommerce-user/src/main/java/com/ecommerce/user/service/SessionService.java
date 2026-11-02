package com.ecommerce.user.service;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.user.config.JwtProperties;
import com.ecommerce.user.dto.response.MeResponse;
import com.ecommerce.user.dto.response.TokenResponse;
import com.ecommerce.user.entity.RefreshToken;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.entity.UserSession;
import com.ecommerce.user.enums.UserStatus;
import com.ecommerce.user.repository.RefreshTokenRepository;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.repository.UserSessionRepository;
import com.ecommerce.user.security.JwtTokenProvider;
import com.ecommerce.user.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Sessions and their tokens. Access JWT: short-lived, bound to a session. Refresh token:
 * opaque, hashed at rest, single use; refreshing rotates it and slides the session expiry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

	private final UserSessionRepository sessionRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	private final JwtTokenProvider tokenProvider;
	private final JwtProperties jwtProperties;
	private final Clock clock;

	@Transactional
	public TokenResponse openSession(User user, String userAgent) {
		Instant now = clock.instant();
		UserSession session = new UserSession();
		session.setPublicId(UUID.randomUUID());
		session.setUserId(user.getId());
		session.setExpiresAt(now.plus(jwtProperties.refreshTokenExpiry()));
		session.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(200, userAgent.length())));
		sessionRepository.save(session);
		return issueTokens(user, session, now);
	}

	@Transactional(noRollbackFor = ResponseException.class)
	public TokenResponse refresh(String rawRefreshToken) {
		Instant now = clock.instant();
		RefreshToken token = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawRefreshToken))
				.orElseThrow(this::invalid);
		UserSession session = sessionRepository.findById(token.getSessionId()).orElseThrow(this::invalid);

		if (token.getUsedAt() != null) {
			// The same refresh token presented twice: one of the two holders stole it.
			log.warn("Refresh token reuse detected; revoking session {} of user {}", session.getId(), session.getUserId());
			revoke(session, now);
			throw invalid();
		}
		if (token.getExpiresAt().isBefore(now) || !session.isUsable(now)) {
			throw invalid();
		}
		User user = userRepository.findById(session.getUserId())
				.filter(u -> u.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(this::invalid);

		token.setUsedAt(now);
		session.setExpiresAt(now.plus(jwtProperties.refreshTokenExpiry()));
		return issueTokens(user, session, now);
	}

	@Transactional
	public void signOut(UUID sessionPublicId) {
		sessionRepository.findByPublicId(sessionPublicId).ifPresent(s -> revoke(s, clock.instant()));
	}

	/** Sign the user out everywhere: on ban, password change and account changes. */
	@Transactional
	public void revokeAll(Long userId) {
		sessionRepository.revokeAllForUser(userId, clock.instant());
		refreshTokenRepository.deleteByUserId(userId);
	}

	private void revoke(UserSession session, Instant now) {
		session.setActive(false);
		session.setRevokedAt(now);
		refreshTokenRepository.deleteBySessionId(session.getId());
	}

	private TokenResponse issueTokens(User user, UserSession session, Instant now) {
		String raw = TokenHasher.newToken();
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setSessionId(session.getId());
		refreshToken.setUserId(user.getId());
		refreshToken.setTokenHash(TokenHasher.sha256(raw));
		refreshToken.setExpiresAt(now.plus(jwtProperties.refreshTokenExpiry()));
		refreshTokenRepository.save(refreshToken);

		String access = tokenProvider.createAccessToken(user.getId(), session.getPublicId());
		return TokenResponse.bearer(access, raw, tokenProvider.accessTokenSeconds(), MeResponse.from(user));
	}

	private ResponseException invalid() {
		return ResponseException.unauthorized(ErrorCode.INVALID_REFRESH_TOKEN);
	}
}
