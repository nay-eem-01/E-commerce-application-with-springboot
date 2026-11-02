package com.ecommerce.user.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

	@ParameterizedTest
	@ValueSource(strings = {"", "short1!", "has space1!", "nodigits!!", "nosymbol123", "12345678!!"})
	void rejectsWeakPasswords(String pw) {
		assertThat(PasswordPolicy.problemWith(pw)).isNotNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {"Senpai-Admin-2026!", "abc123!@"})
	void acceptsStrongPasswords(String pw) {
		assertThat(PasswordPolicy.problemWith(pw)).isNull();
	}
}
