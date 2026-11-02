package com.ecommerce.user.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * JWT settings. The secret has no default in source or in a committed properties file:
 * the application refuses to start without JWT_SECRET (the old code hard-coded it).
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
		@NotBlank String secret,
		@NotNull Duration accessTokenExpiry,
		@NotNull Duration refreshTokenExpiry
) {}
