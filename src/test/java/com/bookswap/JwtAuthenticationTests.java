package com.bookswap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookswap.auth.jwt.JwtProperties;
import com.bookswap.auth.jwt.JwtTokenProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Import({TestcontainersConfiguration.class, JwtAuthenticationTests.ProbeConfig.class})
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=integration-test-secret-key-0123456789")
@DisplayName("JWT 인증 필터")
class JwtAuthenticationTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JwtTokenProvider tokenProvider;

	@TestConfiguration
	static class ProbeConfig {

		@RestController
		static class ProbeController {

			@GetMapping("/api/test/me")
			String me(@AuthenticationPrincipal Long memberId) {
				return String.valueOf(memberId);
			}
		}
	}

	@Test
	@DisplayName("유효한 토큰이면 principal 로 회원 ID가 전달된다")
	void validToken() throws Exception {
		String token = tokenProvider.createAccessToken(7L);

		mockMvc.perform(get("/api/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(content().string("7"));
	}

	@Test
	@DisplayName("만료된 토큰이면 401과 EXPIRED_TOKEN 을 반환한다")
	void expiredToken() throws Exception {
		JwtTokenProvider past = new JwtTokenProvider(
				new JwtProperties("integration-test-secret-key-0123456789", Duration.ofHours(1)),
				Clock.fixed(Instant.now().minus(Duration.ofDays(1)), ZoneOffset.UTC));
		String token = past.createAccessToken(7L);

		mockMvc.perform(get("/api/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("EXPIRED_TOKEN"));
	}

	@Test
	@DisplayName("위조된 토큰이면 401과 INVALID_TOKEN 을 반환한다")
	void invalidToken() throws Exception {
		mockMvc.perform(get("/api/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}

	@Test
	@DisplayName("Bearer 형식이 아닌 헤더는 토큰이 없는 것으로 보고 UNAUTHORIZED 를 반환한다")
	void nonBearerHeader() throws Exception {
		mockMvc.perform(get("/api/test/me").header(HttpHeaders.AUTHORIZATION, "Basic abc"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("공개 경로는 잘못된 토큰이 있어도 차단되지 않는다")
	void publicPathIgnoresBadToken() throws Exception {
		mockMvc.perform(get("/actuator/health").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
				.andExpect(status().isOk());
	}
}
