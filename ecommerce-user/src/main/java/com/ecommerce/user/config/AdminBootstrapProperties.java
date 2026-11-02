package com.ecommerce.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Both optional; see {@code AdminBootstrap}. */
@ConfigurationProperties(prefix = "app.initial-admin")
public record AdminBootstrapProperties(String email, String password) {}
