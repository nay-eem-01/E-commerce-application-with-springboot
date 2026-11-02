package com.ecommerce.api.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Swagger UI at /swagger-ui.html; switched off in production. */
@Configuration
@SecurityScheme(name = "jwtToken", scheme = "bearer", bearerFormat = "JWT", type = SecuritySchemeType.HTTP)
public class SwaggerConfig {

	@Bean
	public OpenAPI openApi(@Value("${app.name}") String appName, @Value("${app.back-end-url}") String backEndUrl) {
		return new OpenAPI()
				.info(new Info().title(appName + " API").version("1.0.0")
						.description("Anime merchandise shop. Every response uses the envelope "
								+ "{status, message, payload, success}; money is BDT with two decimals."))
				.servers(List.of(new Server().url(backEndUrl)));
	}
}
