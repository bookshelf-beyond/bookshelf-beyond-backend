package com.bookswap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("보안 설정")
class SecurityConfigTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("인증 없이 헬스 체크 API에 접근하면 200 OK를 반환한다")
	void healthIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("인증이 필요한 경로에 토큰 없이 요청하면 401과 UNAUTHORIZED 에러 응답을 반환한다")
	void protectedEndpointReturns401WithErrorBody() throws Exception {
		mockMvc.perform(get("/api/books"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("비밀번호는 원문과 다른 해시로 저장되고, 같은 비밀번호도 매번 다른 해시가 나오며, 원문으로 검증된다")
	void passwordEncoderHashesWithSalt() {
		String raw = "Passw0rd#";

		String first = passwordEncoder.encode(raw);
		String second = passwordEncoder.encode(raw);

		assertThat(first).isNotEqualTo(raw).isNotEqualTo(second);
		assertThat(passwordEncoder.matches(raw, first)).isTrue();
		assertThat(passwordEncoder.matches("wrong-Passw0rd#", first)).isFalse();
	}
}
