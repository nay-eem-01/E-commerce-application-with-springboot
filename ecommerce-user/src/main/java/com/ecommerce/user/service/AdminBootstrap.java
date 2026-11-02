package com.ecommerce.user.service;

import com.ecommerce.common.logging.LogMasks;
import com.ecommerce.user.config.AdminBootstrapProperties;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.enums.RoleType;
import com.ecommerce.user.repository.RoleRepository;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Creates the first admin when none exists and INITIAL_ADMIN_EMAIL is set. No default
 * credentials anywhere (the old code seeded admin/adminPass). Without INITIAL_ADMIN_PASSWORD
 * a random one is generated and logged once. Either way the admin must change it on first
 * sign-in.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

	private final AdminBootstrapProperties properties;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (userRepository.existsWithRole(RoleType.ADMIN)) {
			return;
		}
		if (properties.email() == null || properties.email().isBlank()) {
			log.warn("No admin account exists and INITIAL_ADMIN_EMAIL is not set; admin endpoints are unusable.");
			return;
		}
		String email = properties.email().trim().toLowerCase(Locale.ROOT);
		boolean generated = properties.password() == null || properties.password().isBlank();
		String password = generated ? TokenHasher.newToken().substring(0, 20) + "!1a" : properties.password();

		User admin = userRepository.findByEmailIgnoreCase(email).orElseGet(User::new);
		admin.setEmail(email);
		admin.setName("Admin");
		admin.setPasswordHash(passwordEncoder.encode(password));
		admin.setMustChangePassword(true);
		admin.getRoles().add(roleRepository.findByName(RoleType.ADMIN).orElseThrow());
		userRepository.save(admin);

		if (generated) {
			log.warn("Created admin {} with generated password: {} (shown once; change it on first sign-in)",
					LogMasks.maskEmail(email), password);
		} else {
			log.warn("Created admin {} from INITIAL_ADMIN_PASSWORD; change it on first sign-in", LogMasks.maskEmail(email));
		}
	}
}
