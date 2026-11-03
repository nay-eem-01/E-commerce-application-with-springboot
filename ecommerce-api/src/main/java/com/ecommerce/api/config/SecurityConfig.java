package com.ecommerce.api.config;

import com.ecommerce.common.exception.JsonSecurityHandlers;
import com.ecommerce.user.security.JwtAuthenticationFilter;
import com.ecommerce.user.security.PasswordChangeRequiredFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * One stateless filter chain. Deny by default: everything not listed as public needs a valid
 * token (the old config ended in "/api/**" permitAll, which opened every endpoint).
 * Admin endpoints are additionally guarded by a privilege in @PreAuthorize, and every
 * ownership rule is checked again in the service.
 *
 * CSRF is off because auth is a bearer header, not a cookie. If cookie auth is ever added,
 * CSRF protection comes back with it.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private static final String[] PUBLIC_GET = {
			"/api/categories", "/api/characters", "/api/products", "/api/products/**", "/api/checkout/delivery-options",
			"/api/payments/*/callback",
			"/actuator/health", "/actuator/health/**", "/actuator/info",
			"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
	};

	private static final String[] PUBLIC_POST = {
			"/api/auth/otp/request", "/api/auth/otp/verify", "/api/auth/sign-in", "/api/auth/refresh-token",
			"/api/checkout/quote", "/api/orders", "/api/orders/track", "/api/orders/pay", "/api/early-access",
			// Payment providers post here without our token; every call is verified with the provider.
			"/api/payments/*/callback", "/api/payments/*/ipn"
	};

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final PasswordChangeRequiredFilter passwordChangeRequiredFilter;
	private final JsonSecurityHandlers jsonSecurityHandlers;
	private final CorsProperties corsProperties;

	@Bean
	public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
						.requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
						.requestMatchers("/error").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(e -> e
						.authenticationEntryPoint(jsonSecurityHandlers)
						.accessDeniedHandler(jsonSecurityHandlers))
				.headers(h -> h
						.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
						.frameOptions(f -> f.deny()))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(passwordChangeRequiredFilter, JwtAuthenticationFilter.class);
		return http.build();
	}

	/** The filters are @Components; stop Boot from also running them as plain servlet filters. */
	@Bean
	public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
		FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(jwtAuthenticationFilter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	public FilterRegistrationBean<PasswordChangeRequiredFilter> passwordChangeFilterRegistration() {
		FilterRegistrationBean<PasswordChangeRequiredFilter> registration = new FilterRegistrationBean<>(passwordChangeRequiredFilter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(corsProperties.allowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Correlation-Id"));
		configuration.setExposedHeaders(List.of("X-Correlation-Id"));
		configuration.setAllowCredentials(false);
		configuration.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
