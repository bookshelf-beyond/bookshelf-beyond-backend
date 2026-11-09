package com.bookswap.auth.jwt;

import com.bookswap.global.error.BusinessException;
import com.bookswap.global.error.ErrorCode;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/**
 * 액세스 토큰(HS256) 발급·검증. subject 에 회원 ID 를 담는다.
 * 서명은 디코더가 검증하고, 만료는 직접 판정해서 EXPIRED_TOKEN 과 INVALID_TOKEN 을 구분한다.
 */
@Component
public class JwtTokenProvider {

	private static final int MIN_SECRET_BYTES = 32;

	private final JwtEncoder encoder;
	private final JwtDecoder decoder;
	private final JwtProperties properties;
	private final Clock clock;

	public JwtTokenProvider(JwtProperties properties, Clock clock) {
		byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
		if (secret.length < MIN_SECRET_BYTES) {
			throw new IllegalArgumentException("jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes");
		}
		SecretKey key = new SecretKeySpec(secret, "HmacSHA256");
		this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
		NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
		// 기본 검증기는 만료를 일반 오류로 던지므로 끄고, 만료는 parse() 에서 직접 판정한다.
		nimbusDecoder.setJwtValidator(token -> OAuth2TokenValidatorResult.success());
		this.decoder = nimbusDecoder;
		this.properties = properties;
		this.clock = clock;
	}

	public String createAccessToken(long memberId) {
		Instant now = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(Long.toString(memberId))
				.issuedAt(now)
				.expiresAt(now.plus(properties.accessTokenExpiration()))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	/**
	 * @return 토큰에 담긴 회원 ID
	 * @throws BusinessException 서명 불일치·형식 오류는 INVALID_TOKEN, 만료는 EXPIRED_TOKEN
	 */
	public long parseMemberId(String token) {
		Jwt jwt;
		try {
			jwt = decoder.decode(token);
		} catch (JwtException e) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}
		Instant expiresAt = jwt.getExpiresAt();
		if (expiresAt == null) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}
		if (!expiresAt.isAfter(clock.instant())) {
			throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
		}
		try {
			return Long.parseLong(jwt.getSubject());
		} catch (NumberFormatException e) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}
	}
}
