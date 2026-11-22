package com.ecommerce.user.security;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Verifies Google Identity Services ID tokens using Google's cached signing keys. */
@Component
public class GoogleIdTokenVerifier {

	private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
	private final NimbusJwtDecoder decoder;

	@Autowired
	public GoogleIdTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
		this(clientId, clientId.isBlank() ? null : NimbusJwtDecoder
				.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build());
	}

	/** Lets tests sign tokens with their own key while exercising the production validators. */
	GoogleIdTokenVerifier(String clientId, NimbusJwtDecoder decoder) {
		this.decoder = decoder;
		if (decoder == null) {
			return;
		}
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), jwt -> {
			if (jwt.getExpiresAt() != null && ISSUERS.contains(jwt.getClaimAsString("iss"))
					&& jwt.getAudience() != null && jwt.getAudience().contains(clientId)) {
				return OAuth2TokenValidatorResult.success();
			}
			return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
		}));
	}

	public Identity verify(String token) {
		if (decoder == null) {
			throw new ResponseException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.GOOGLE_SIGN_IN_DISABLED);
		}
		try {
			Jwt jwt = decoder.decode(token);
			String email = jwt.getClaimAsString("email");
			String subject = jwt.getSubject();
			Object verified = jwt.getClaims().get("email_verified");
			if (email == null || email.isBlank() || email.length() > 254 || subject == null
					|| subject.isBlank() || subject.length() > 255
					|| !(Boolean.TRUE.equals(verified) || "true".equals(verified))) {
				throw ResponseException.unauthorized(ErrorCode.INVALID_CREDENTIALS);
			}
			return new Identity(subject, email, jwt.getClaimAsString("name"), jwt.getClaimAsString("hd") != null);
		} catch (JwtException e) {
			throw ResponseException.unauthorized(ErrorCode.INVALID_CREDENTIALS);
		}
	}

	public record Identity(String subject, String email, String name, boolean hosted) {}
}
