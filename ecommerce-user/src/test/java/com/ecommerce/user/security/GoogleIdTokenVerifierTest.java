package com.ecommerce.user.security;

import com.ecommerce.common.exception.ResponseException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleIdTokenVerifierTest {

	private static KeyPair googleKey;

	@BeforeAll
	static void createSigningKey() throws Exception {
		var generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		googleKey = generator.generateKeyPair();
	}

	private final GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier("shop-client",
			NimbusJwtDecoder.withPublicKey((RSAPublicKey) googleKey.getPublic()).build());

	@Test
	void acceptsVerifiedGoogleIdentityIssuedForThisShop() throws Exception {
		var identity = verifier.verify(signedToken("https://accounts.google.com", "shop-client", true, 300));
		assertThat(identity.subject()).isEqualTo("shopper-subject");
		assertThat(identity.email()).isEqualTo("shopper@gmail.com");
	}

	@Test
	void rejectsOtherAudiencesIssuersExpiredTokensAndUnverifiedEmails() throws Exception {
		rejects(signedToken("https://accounts.google.com", "another-client", true, 300));
		rejects(signedToken("https://example.com", "shop-client", true, 300));
		rejects(signedToken("https://accounts.google.com", "shop-client", true, -300));
		rejects(signedToken("https://accounts.google.com", "shop-client", false, 300));
	}

	private void rejects(String credential) {
		assertThatThrownBy(() -> verifier.verify(credential))
				.isInstanceOfSatisfying(ResponseException.class,
						error -> assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	private static String signedToken(String issuer, String audience, boolean verified, long expiresIn) throws Exception {
		var claims = new JWTClaimsSet.Builder()
				.issuer(issuer)
				.audience(audience)
				.subject("shopper-subject")
				.expirationTime(Date.from(Instant.now().plusSeconds(expiresIn)))
				.claim("email", "shopper@gmail.com")
				.claim("email_verified", verified)
				.build();
		var token = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
		token.sign(new RSASSASigner(googleKey.getPrivate()));
		return token.serialize();
	}
}
