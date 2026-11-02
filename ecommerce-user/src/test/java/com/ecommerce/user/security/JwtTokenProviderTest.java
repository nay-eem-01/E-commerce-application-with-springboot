package com.ecommerce.user.security;

import com.ecommerce.user.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

	private static String secret(int bytes) {
		byte[] b = new byte[bytes];
		new SecureRandom().nextBytes(b);
		return Base64.getEncoder().encodeToString(b);
	}

	private static JwtTokenProvider provider(String secret, Duration access) {
		return new JwtTokenProvider(new JwtProperties(secret, access, Duration.ofDays(30)));
	}

	@Test
	void refusesToStartWithAWeakSecret() {
		assertThatThrownBy(() -> provider(secret(32), Duration.ofMinutes(15)))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("openssl rand -base64 64");
	}

	@Test
	void roundTripCarriesUserAndSession() {
		JwtTokenProvider p = provider(secret(64), Duration.ofMinutes(15));
		UUID sid = UUID.randomUUID();
		var claims = p.parse(p.createAccessToken(42L, sid)).orElseThrow();
		assertThat(claims.userId()).isEqualTo(42L);
		assertThat(claims.sessionId()).isEqualTo(sid);
	}

	@Test
	void rejectsTamperedExpiredForeignAndNonAccessTokensWithoutThrowing() {
		String s = secret(64);
		JwtTokenProvider p = provider(s, Duration.ofMinutes(15));
		String token = p.createAccessToken(1L, UUID.randomUUID());

		assertThat(p.parse(token.substring(0, token.length() - 3) + "abc")).isEmpty();
		assertThat(p.parse("not.a.jwt")).isEmpty();
		assertThat(provider(secret(64), Duration.ofMinutes(15)).parse(token)).isEmpty();
		assertThat(provider(s, Duration.ofMillis(-1000)).parse(provider(s, Duration.ofMillis(-1000)).createAccessToken(1L, UUID.randomUUID()))).isEmpty();

		// same key, valid signature, but not an access token (no typ=access)
		String other = Jwts.builder().subject("1").claim("sid", UUID.randomUUID().toString())
				.expiration(new Date(System.currentTimeMillis() + 60_000))
				.signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(s)), Jwts.SIG.HS512).compact();
		assertThat(p.parse(other)).isEmpty();
	}
}
