package com.ecommerce.order.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

/** Delivery charges and the districts we deliver to. Changing a fee is a config change, not a code change. */
@Validated
@ConfigurationProperties(prefix = "app.delivery")
public record DeliveryProperties(
		@NotNull BigDecimal insideFee,
		@NotNull BigDecimal outsideFee,
		@NotNull BigDecimal freeThreshold,
		@NotEmpty List<String> insideDistricts,
		@NotEmpty List<String> districts,
		@NotNull String insideEstimate,
		@NotNull String outsideEstimate
) {}
