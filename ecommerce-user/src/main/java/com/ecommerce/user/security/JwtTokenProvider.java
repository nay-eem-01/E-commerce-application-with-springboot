package com.ecommerce.user.security;

import com.ecommerce.user.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Access tokens only (refresh tokens are opaque and live in the database). Claims: subject
 * = user id, sid = session public id, typ = access, a unique jti. A JWT is signed, not
 * encrypted, so it carries nothing personal.
 */
@Slf4j
@Component
public class JwtTokenProvider {

	/** HS512 needs a key of at least 512 bits. */
	static final int MIN_SECRET_BYTES = 64;
	static final String CLAIM_SESSION = "sid";
	static final String CLAIM_TYPE = "typ";
	static final String TYPE_ACCESS = "access";

	private final JwtProperties properties;
	private final SecretKey key;
	private final JwtParser parser;

	/** Hand-written: it validates the secret's strength and refuses to start on a weak one. */
	public JwtTokenProvider(JwtProperties properties) {
		byte[] secret = Decoders.BASE64.decode(properties.secret());
		if (secret.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException("JWT_SECRET decodes to " + secret.length
					+ " bytes; HS512 needs at least " + MIN_SECRET_BYTES + ". Generate one with: openssl rand -base64 64");
		}
		this.properties = properties;
		this.key = Keys.hmacShaKeyFor(secret);
		this.parser = Jwts.parser().verifyWith(key).build();
	}

	public String createAccessToken(Long userId, UUID sessionPublicId) {
		Date now = new Date();
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.id(UUID.randomUUID().toString())
				.claim(CLAIM_SESSION, sessionPublicId.toString())
				.claim(CLAIM_TYPE, TYPE_ACCESS)
				.issuedAt(now)
				.expiration(new Date(now.getTime() + properties.accessTokenExpiry().toMillis()))
				.signWith(key, Jwts.SIG.HS512)
				.compact();
	}

	public long accessTokenSeconds() {
		return properties.accessTokenExpiry().toSeconds();
	}

	/** Empty for anything that is not a valid, unexpired access token. Never throws. */
	public Optional<AccessClaims> parse(String token) {
		try {
			Claims claims = parser.parseSignedClaims(token).getPayload();
			if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
				return Optional.empty();
			}
			return Optional.of(new AccessClaims(
					Long.valueOf(claims.getSubject()),
					UUID.fromString(claims.get(CLAIM_SESSION, String.class))
			));
		} catch (JwtException | IllegalArgumentException e) {
			log.debug("Rejected access token: {}", e.getClass().getSimpleName());
			return Optional.empty();
		}
	}

	public record AccessClaims(Long userId, UUID sessionId) {}
}
