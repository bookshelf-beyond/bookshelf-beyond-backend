package com.bookswap.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 비밀번호 정책: 영문·숫자·특수문자를 각각 1자 이상 포함하고, 공백 없는 ASCII 문자 8~72자.
 * 72자 상한은 BCrypt 가 처리하는 최대 길이(72바이트)에 맞춘 것이다. null 은 허용하지 않는다.
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

	String message() default "비밀번호는 영문, 숫자, 특수문자를 포함해 8~72자여야 합니다.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
