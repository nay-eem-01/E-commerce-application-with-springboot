package com.ecommerce.user.service;

import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.ratelimit.InMemoryRateLimiter;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.enums.UserStatus;
import com.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

	private final UserRepository users = mock(UserRepository.class);
	private final SessionService sessions = mock(SessionService.class);
	private final UserService service = new UserService(users, mock(PasswordEncoder.class), sessions, new InMemoryRateLimiter());

	/** Regression: a null filter reached PostgreSQL as bytea inside lower() and the admin list 500'd. */
	@Test
	void anEmptySearchBindsAnEmptyStringNeverNull() {
		when(users.search(any(), any())).thenReturn(Page.empty());
		service.search(null, 0, 20, "creationDate", AscOrDescType.desc);
		verify(users).search(eq(""), any());
	}

	@Test
	void banningSignsTheUserOutEverywhere() {
		User u = new User();
		u.setId(5L);
		when(users.findById(5L)).thenReturn(Optional.of(u));
		service.updateStatus(1L, 5L, UserStatus.BANNED);
		assertThat(u.getStatus()).isEqualTo(UserStatus.BANNED);
		verify(sessions).revokeAll(5L);
	}

	@Test
	void anAdminCannotBanThemself() {
		assertThatThrownBy(() -> service.updateStatus(1L, 1L, UserStatus.BANNED)).isInstanceOf(ResponseException.class);
	}
}
