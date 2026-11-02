package com.ecommerce.order.config;

import com.ecommerce.order.enums.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

/**
 * Methods offered at checkout. Only COD has a working implementation; bKash, Nagad and card
 * are switched on here once their gateway class exists (see PaymentGateway).
 */
@Validated
@ConfigurationProperties(prefix = "app.payment")
public record PaymentProperties(@NotEmpty Set<PaymentMethod> enabledMethods) {}
