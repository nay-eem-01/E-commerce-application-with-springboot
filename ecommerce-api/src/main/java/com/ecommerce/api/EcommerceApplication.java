package com.ecommerce.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * The only deployable. Scans every module under com.ecommerce. Boot's in-memory "user" with a
 * generated password is excluded: authentication is JWT-only.
 */
@SpringBootApplication(scanBasePackages = "com.ecommerce", exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan("com.ecommerce")
@EntityScan("com.ecommerce")
@EnableJpaRepositories("com.ecommerce")
public class EcommerceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcommerceApplication.class, args);
	}
}
