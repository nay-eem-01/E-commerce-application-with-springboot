package com.ecommerce.user.service;

import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.user.config.JwtProperties;
import com.ecommerce.user.entity.RefreshToken;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.entity.UserSession;
import com.ecommerce.user.enums.UserStatus;
import com.ecommerce.user.repository.RefreshTokenRepository;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.repository.UserSessionRepository;
import com.ecommerce.user.security.JwtTokenProvider;
import com.ecommerce.user.security.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");

	private final UserSessionRepository sessions = mock(UserSessionRepository.class);
	private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
	private final UserRepository users = mock(UserRepository.class);
	private SessionService service;
	private UserSession session;
	private RefreshToken token;
	private User user;

	@BeforeEach
	void setUp() {
		JwtProperties props = new JwtProperties(Base64.getEncoder().encodeToString(new byte[64]), Duration.ofMinutes(15), Duration.ofDays(30));
		service = new SessionService(sessions, tokens, users, new JwtTokenProvider(props), props, Clock.fixed(NOW, ZoneOffset.UTC));

		session = new UserSession();
		session.setId(7L);
		session.setUserId(1L);
		session.setPublicId(UUID.randomUUID());
		session.setExpiresAt(NOW.plus(Duration.ofDays(5)));
		token = new RefreshToken();
		token.setSessionId(7L);
		token.setUserId(1L);
		token.setExpiresAt(NOW.plus(Duration.ofDays(5)));
		user = new User();
		user.setId(1L);
		user.setStatus(UserStatus.ACTIVE);

		when(tokens.findByTokenHash(TokenHasher.sha256("raw"))).thenReturn(Optional.of(token));
		when(sessions.findById(7L)).thenReturn(Optional.of(session));
		when(users.findById(1L)).thenReturn(Optional.of(user));
	}

	@Test
	void refreshMarksTheOldTokenUsedAndSlidesTheSession() {
		var response = service.refresh("raw");
		assertThat(token.getUsedAt()).isEqualTo(NOW);
		assertThat(session.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
		assertThat(response.refreshToken()).isNotEqualTo("raw");
		verify(tokens).save(any(RefreshToken.class));
	}

	@Test
	void aSecondUseOfTheSameTokenRevokesTheWholeSession() {
		token.setUsedAt(NOW.minusSeconds(30));
		assertThatThrownBy(() -> service.refresh("raw"))
				.isInstanceOf(ResponseException.class)
				.extracting(e -> ((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(session.isActive()).isFalse();
		verify(tokens).deleteBySessionId(7L);
	}

	@Test
	void expiredTokenRevokedSessionOrBannedUserAreRejected() {
		token.setExpiresAt(NOW.minusSeconds(1));
		assertThatThrownBy(() -> service.refresh("raw")).isInstanceOf(ResponseException.class);

		token.setExpiresAt(NOW.plusSeconds(60));
		session.setActive(false);
		assertThatThrownBy(() -> service.refresh("raw")).isInstanceOf(ResponseException.class);

		session.setActive(true);
		user.setStatus(UserStatus.BANNED);
		assertThatThrownBy(() -> service.refresh("raw")).isInstanceOf(ResponseException.class);
		verify(tokens, never()).save(any());
	}

	@Test
	void unknownTokenIs401() {
		assertThatThrownBy(() -> service.refresh("someone-elses")).isInstanceOf(ResponseException.class);
	}
}
