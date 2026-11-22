package com.ecommerce.user.service;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.ratelimit.RateLimiter;
import com.ecommerce.user.dto.response.TokenResponse;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.enums.RoleType;
import com.ecommerce.user.enums.UserStatus;
import com.ecommerce.user.repository.RoleRepository;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.security.GoogleIdTokenVerifier;
import com.ecommerce.user.security.PasswordPolicy;
import com.ecommerce.user.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ShopperAuthService {

	public enum Link {
		REGISTER("verify-email", "Finish creating your Senpai Supply account"),
		RESET("reset-password", "Reset your Senpai Supply password");

		private final String path;
		private final String subject;

		Link(String path, String subject) {
			this.path = path;
			this.subject = subject;
		}
	}

	private final UserRepository users;
	private final RoleRepository roles;
	private final SessionService sessions;
	private final GoogleIdTokenVerifier google;
	private final RateLimiter limits;
	private final PasswordEncoder passwords;
	private final ObjectProvider<JavaMailSender> mail;
	private final JdbcTemplate db;
	private final ApplicationEventPublisher events;
	private final Clock clock;

	@Value("${app.mail.storefront-url}")
	private String storefrontUrl;

	@Transactional
	public void emailLink(String address, Link purpose, String ip) {
		String email = normalize(address);
		limits.hit("shopper-link:ip:" + ip, 20, Duration.ofHours(1));
		limits.hit("shopper-link:email:" + email, 5, Duration.ofHours(1));
		if (mail.getIfAvailable() == null) {
			throw new ResponseException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.SHOPPER_EMAIL_UNAVAILABLE);
		}
		User user = users.findByEmailIgnoreCase(email).orElse(null);
		if (purpose == Link.REGISTER && user != null) {
			return;
		}
		if (purpose == Link.RESET && (user == null || !isCustomer(user)
				|| user.getEmailVerifiedAt() == null || user.getStatus() != UserStatus.ACTIVE)) {
			return;
		}
		String token = TokenHasher.newToken();
		db.update("""
				INSERT INTO shopper_email_links (email, purpose, token_hash, expires_at)
				VALUES (?, ?, ?, CURRENT_TIMESTAMP + INTERVAL '30 minutes')
				ON CONFLICT (email, purpose) DO UPDATE
				SET token_hash = EXCLUDED.token_hash, expires_at = EXCLUDED.expires_at
				""", email, purpose.name(), TokenHasher.sha256(token));
		events.publishEvent(new ShopperAuthEmail(email, purpose.subject,
				purpose.subject + " using this link. It expires in 30 minutes.\n\n"
						+ storefrontUrl.replaceAll("/+$", "") + "/" + purpose.path + "#token=" + token));
	}

	@Transactional
	public TokenResponse finishEmail(String token, String password, Link purpose, String ip, String agent) {
		limits.hit("shopper-link:verify:" + ip, 30, Duration.ofMinutes(15));
		String problem = PasswordPolicy.problemWith(password);
		if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw ResponseException.badRequest(ErrorCode.SHOPPER_PASSWORD_TOO_LONG);
		}
		if (problem != null) {
			throw ResponseException.badRequest(problem);
		}
		var emails = db.queryForList("""
				DELETE FROM shopper_email_links
				WHERE token_hash = ? AND purpose = ? AND expires_at > CURRENT_TIMESTAMP
				RETURNING email
				""", String.class, TokenHasher.sha256(token), purpose.name());
		if (emails.isEmpty()) {
			throw ResponseException.badRequest(ErrorCode.INVALID_EMAIL_LINK);
		}
		String email = emails.getFirst();
		User user = users.findByEmailIgnoreCase(email).orElse(null);
		if (purpose == Link.REGISTER) {
			if (user != null) {
				throw ResponseException.badRequest(ErrorCode.INVALID_EMAIL_LINK);
			}
			user = newCustomer(email);
		} else {
			requireActiveCustomer(user);
			if (user.getEmailVerifiedAt() == null) {
				throw ResponseException.badRequest(ErrorCode.INVALID_EMAIL_LINK);
			}
			sessions.revokeAll(user.getId());
		}
		user.setPasswordHash(passwords.encode(password));
		return sessions.openSession(users.saveAndFlush(user), agent);
	}

	@Transactional
	public TokenResponse signInWithGoogle(String token, String ip, String agent) {
		limits.hit("shopper-google:ip:" + ip, 30, Duration.ofMinutes(15));
		var identity = google.verify(token);
		String email = normalize(identity.email());
		boolean trustedEmail = identity.hosted() || email.endsWith("@gmail.com");
		User user = users.findByGoogleSubject(identity.subject()).orElse(null);
		if (user == null) {
			user = users.findByEmailIgnoreCase(email).orElse(null);
			if (user == null) {
				// Google does not continuously verify ownership of third-party email addresses.
				if (!trustedEmail) {
					throw ResponseException.unauthorized(ErrorCode.INVALID_CREDENTIALS);
				}
				user = newCustomer(email);
				String name = identity.name();
				if (name != null && name.length() <= 80) {
					user.setName(name);
				}
				user.setGoogleSubject(identity.subject());
				users.saveAndFlush(user);
			} else {
				requireActiveCustomer(user);
				if (!trustedEmail || users.linkGoogle(user.getId(), email, identity.subject()) != 1) {
					throw ResponseException.unauthorized(ErrorCode.INVALID_CREDENTIALS);
				}
			}
		}
		requireActiveCustomer(user);
		return sessions.openSession(user, agent);
	}

	private User newCustomer(String email) {
		User user = new User();
		user.setEmail(email);
		user.setEmailVerifiedAt(clock.instant());
		user.getRoles().add(roles.findByName(RoleType.CUSTOMER).orElseThrow());
		return user;
	}

	private static void requireActiveCustomer(User user) {
		if (user == null || !isCustomer(user)) {
			throw ResponseException.unauthorized(ErrorCode.INVALID_CREDENTIALS);
		}
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw ResponseException.forbidden(ErrorCode.ACCOUNT_BANNED);
		}
	}

	private static boolean isCustomer(User user) {
		return user.getRoles().size() == 1 && user.hasRole(RoleType.CUSTOMER);
	}

	private static String normalize(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}
}
