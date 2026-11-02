package com.ecommerce.notification.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * provider: "smtp" sends through spring.mail.*; "log" writes emails to the log (development).
 * Switching is one property (MAIL_PROVIDER). storefrontUrl builds the links in emails.
 * staffAddress (optional) receives a copy of every new order.
 */
@Validated
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
		@NotBlank String provider,
		@NotBlank String from,
		@NotBlank String brandName,
		@NotBlank String storefrontUrl,
		String staffAddress,
		@NotNull Duration dispatchInterval,
		@NotNull Duration retention
) {}
