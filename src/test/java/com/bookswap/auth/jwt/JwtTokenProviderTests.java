package com.bookswap.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookswap.global.error.BusinessException;
import com.bookswap.global.error.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JWT 토큰 제공자")
class JwtTokenProviderTests {

	private static final String SECRET = "unit-test-secret-key-0123456789-abcdef";
	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	private static final Duration TTL = Duration.ofHours(1);

	private JwtTokenProvider providerAt(Instant instant, String secret) {
		return new JwtTokenProvider(new JwtProperties(secret, TTL), Clock.fixed(instant, ZoneOffset.UTC));
	}

	@Test
	@DisplayName("발급한 토큰을 파싱하면 회원 ID를 돌려준다")
	void roundTrip() {
		JwtTokenProvider provider = providerAt(NOW, SECRET);

		String token = provider.createAccessToken(42L);

		assertThat(provider.parseMemberId(token)).isEqualTo(42L);
	}

	@Test
	@DisplayName("만료 시각이 지난 토큰은 EXPIRED_TOKEN 예외를 던진다")
	void expired() {
		String token = providerAt(NOW, SECRET).createAccessToken(1L);
		JwtTokenProvider later = providerAt(NOW.plus(TTL).plusSeconds(1), SECRET);

		assertThatThrownBy(() -> later.parseMemberId(token))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EXPIRED_TOKEN));
	}

	@Test
	@DisplayName("만료 시각 정각의 토큰도 만료로 본다")
	void expiredExactlyAtBoundary() {
		String token = providerAt(NOW, SECRET).createAccessToken(1L);
		JwtTokenProvider atBoundary = providerAt(NOW.plus(TTL), SECRET);

		assertThatThrownBy(() -> atBoundary.parseMemberId(token))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EXPIRED_TOKEN));
	}

	@Test
	@DisplayName("다른 키로 서명된 토큰은 INVALID_TOKEN 예외를 던진다")
	void signedWithOtherKey() {
		String token = providerAt(NOW, "another-secret-key-0123456789-abcdefgh").createAccessToken(1L);
		JwtTokenProvider provider = providerAt(NOW, SECRET);

		assertThatThrownBy(() -> provider.parseMemberId(token))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
	}

	@Test
	@DisplayName("형식이 깨진 토큰은 INVALID_TOKEN 예외를 던진다")
	void malformed() {
		JwtTokenProvider provider = providerAt(NOW, SECRET);

		assertThatThrownBy(() -> provider.parseMemberId("not-a-jwt"))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
	}

	@Test
	@DisplayName("32바이트 미만 시크릿은 시작 시점에 거부한다")
	void shortSecretRejected() {
		assertThatThrownBy(() -> providerAt(NOW, "too-short"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
