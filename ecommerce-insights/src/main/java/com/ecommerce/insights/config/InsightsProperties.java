package com.ecommerce.insights.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/** Signal thresholds. Tuning a signal is a config change, not a code change. */
@Validated
@ConfigurationProperties(prefix = "app.insights")
public record InsightsProperties(
		@Min(1) int restockDays,
		@Min(1) int wantedSoldOutMin,
		@Min(1) int lookedViewsMin,
		@NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal lookedConversionMax,
		@Min(1) int searchGapMin,
		@Min(1) int signalLimit,
		@Min(1) int productLimit
) {}
