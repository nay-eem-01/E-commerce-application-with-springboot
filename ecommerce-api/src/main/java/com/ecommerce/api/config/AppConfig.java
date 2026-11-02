package com.ecommerce.api.config;

import com.ecommerce.common.model.AuditorAwareImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

@Configuration
/** Scheduling runs the payment expiry check (one instance; add a lock before scaling out). */
@EnableScheduling
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AppConfig {

	@Bean
	public AuditorAware<String> auditorAware() {
		return new AuditorAwareImpl();
	}

	/** Injected wherever "now" matters, so tests can pin time. */
	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
