package com.bookswap.auth.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("비밀번호 정책")
class ValidPasswordTests {

	private final PasswordValidator validator = new PasswordValidator();

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@ValueSource(strings = {
			"abcd123!",          // 정확히 8자(하한)
			"Passw0rd#",
			"!1aaaaaa",          // 특수문자·숫자가 앞에 와도 통과
	})
	@DisplayName("영문·숫자·특수문자를 모두 포함한 8자 이상이면 통과한다")
	void acceptsPolicyCompliant(String password) {
		assertThat(validator.isValid(password, null)).isTrue();
	}

	@Test
	@DisplayName("정확히 72자는 통과하고 73자는 거부한다")
	void upperBoundary() {
		String base = "a1!";
		assertThat(validator.isValid(base + "x".repeat(69), null)).isTrue();   // 72자
		assertThat(validator.isValid(base + "x".repeat(70), null)).isFalse();  // 73자
	}

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@ValueSource(strings = {
			"abc12!",            // 8자 미만
			"abcdefg1",          // 특수문자 없음
			"abcdefg!",          // 숫자 없음
			"1234567!",          // 영문 없음
			"abcd 123!",         // 공백 포함
			"abcd123!한",        // ASCII 이외 문자 포함
			"",
	})
	@DisplayName("조건을 하나라도 어기면 거부한다")
	void rejectsViolations(String password) {
		assertThat(validator.isValid(password, null)).isFalse();
	}

	@ParameterizedTest
	@NullSource
	@DisplayName("null 은 거부한다")
	void rejectsNull(String password) {
		assertThat(validator.isValid(password, null)).isFalse();
	}

	@Test
	@DisplayName("어노테이션을 붙인 필드는 검증 실패 시 기본 메시지를 돌려준다")
	void annotationWiring() {
		record Request(@ValidPassword String password) {
		}
		Validator beanValidator = Validation.buildDefaultValidatorFactory().getValidator();

		var violations = beanValidator.validate(new Request("short"));

		assertThat(violations).singleElement().satisfies(v ->
				assertThat(v.getMessage()).isEqualTo("비밀번호는 영문, 숫자, 특수문자를 포함해 8~72자여야 합니다."));
		assertThat(beanValidator.validate(new Request("Passw0rd#"))).isEmpty();
	}
}
