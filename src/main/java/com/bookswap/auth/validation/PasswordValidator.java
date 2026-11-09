package com.bookswap.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

	// 공백 없는 ASCII 출력 문자(0x21~0x7E) 8~72자 + 영문/숫자/특수문자 각 1자 이상
	private static final Pattern POLICY = Pattern.compile(
			"^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d])[\\x21-\\x7E]{8,72}$");

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value != null && POLICY.matcher(value).matches();
	}
}
