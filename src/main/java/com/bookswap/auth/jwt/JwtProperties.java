package com.bookswap.auth.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("jwt")
public record JwtProperties(
		@NotBlank String secret,
		@NotNull Duration accessTokenExpiration) {
}
