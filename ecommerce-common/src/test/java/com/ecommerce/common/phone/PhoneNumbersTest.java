package com.ecommerce.common.phone;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumbersTest {

	@ParameterizedTest
	@ValueSource(strings = {"01712345678", "+8801712345678", "8801712345678", "017-1234-5678", " 01712 345678 "})
	void acceptsEveryCommonWayOfWritingTheSameNumber(String raw) {
		assertThat(PhoneNumbers.normalize(raw)).contains("+8801712345678");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "12345", "01212345678", "0171234567", "017123456789", "+1 555 123 4567", "0171234567a"})
	void rejectsAnythingThatIsNotABangladeshiMobile(String raw) {
		assertThat(PhoneNumbers.normalize(raw)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"+8801712345678"})
	void showsTheLocalForm(String canonical) {
		assertThat(PhoneNumbers.toLocal(canonical)).isEqualTo("01712345678");
	}
}
