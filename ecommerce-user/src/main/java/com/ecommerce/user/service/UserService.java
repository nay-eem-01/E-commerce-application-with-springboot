package com.ecommerce.user.service;

import com.ecommerce.common.constant.AppConstants;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.model.PageResponse;
import com.ecommerce.common.ratelimit.RateLimiter;
import com.ecommerce.common.web.Pages;
import com.ecommerce.user.dto.request.ChangePasswordRequest;
import com.ecommerce.user.dto.request.UpdateMeRequest;
import com.ecommerce.user.dto.response.AdminUserResponse;
import com.ecommerce.user.dto.response.MeResponse;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.enums.UserStatus;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

	private static final Set<String> ADMIN_SORTS = Set.of(AppConstants.SORT_BY_VALUE, "name");

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final SessionService sessionService;
	private final RateLimiter rateLimiter;

	@Transactional(readOnly = true)
	public MeResponse me(Long userId) {
		return MeResponse.from(find(userId));
	}

	@Transactional
	public MeResponse updateMe(Long userId, UpdateMeRequest request) {
		User user = find(userId);
		user.setName(request.getName().trim());
		String email = request.getEmail() == null || request.getEmail().isBlank()
				? null : request.getEmail().trim().toLowerCase(Locale.ROOT);
		if (email != null && userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
			throw ResponseException.conflict(ErrorCode.EMAIL_TAKEN);
		}
		// Staff sign in with their email, so they cannot clear it here.
		if (email != null || user.getPasswordHash() == null) {
			user.setEmail(email);
		}
		return MeResponse.from(user);
	}

	@Transactional
	public void changePassword(Long userId, ChangePasswordRequest request) {
		rateLimiter.hit("change-password:user:" + userId, 5, Duration.ofMinutes(15));
		User user = find(userId);
		if (user.getPasswordHash() == null) {
			throw ResponseException.badRequest(ErrorCode.PASSWORD_NOT_SET);
		}
		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
			throw ResponseException.badRequest(ErrorCode.WRONG_CURRENT_PASSWORD);
		}
		String problem = PasswordPolicy.problemWith(request.getNewPassword());
		if (problem != null) {
			throw ResponseException.badRequest(problem);
		}
		user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		user.setMustChangePassword(false);
		sessionService.revokeAll(userId);
		log.info("Password changed for user {}; all sessions revoked", userId);
	}

	/** Used by the order flow: a phone-only customer gets the name they typed at checkout. */
	@Transactional
	public void fillNameIfMissing(Long userId, String name) {
		userRepository.findById(userId)
				.filter(u -> u.getName() == null && name != null && !name.isBlank())
				.ifPresent(u -> u.setName(name.trim()));
	}

	@Transactional(readOnly = true)
	public PageResponse<AdminUserResponse> search(String q, int pageNo, int pageSize, String sortBy, AscOrDescType dir) {
		// '' means "no filter" (a null would reach PostgreSQL as bytea inside lower()).
		String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
		return PageResponse.of(userRepository.search(query, Pages.of(pageNo, pageSize, sortBy, dir, ADMIN_SORTS)),
				AdminUserResponse::from);
	}

	@Transactional
	public AdminUserResponse updateStatus(Long actorId, Long userId, UserStatus status) {
		if (actorId.equals(userId)) {
			throw ResponseException.badRequest(ErrorCode.CANNOT_BAN_SELF);
		}
		User user = find(userId);
		user.setStatus(status);
		if (status == UserStatus.BANNED) {
			sessionService.revokeAll(userId);
		}
		log.warn("User {} set to {} by admin {}", userId, status, actorId);
		return AdminUserResponse.from(user);
	}

	private User find(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> ResponseException.notFound(ErrorCode.USER_NOT_FOUND));
	}
}
