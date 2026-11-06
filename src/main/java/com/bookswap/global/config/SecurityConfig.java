package com.bookswap.global.config;

import com.bookswap.auth.jwt.JwtAuthenticationFilter;
import com.bookswap.auth.jwt.JwtProperties;
import com.bookswap.auth.jwt.JwtTokenProvider;
import com.bookswap.global.error.ErrorCode;
import com.bookswap.global.error.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http, ObjectMapper objectMapper, JwtTokenProvider jwtTokenProvider) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/api/auth/**").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(entryPoint(objectMapper))
						.accessDeniedHandler(accessDenied(objectMapper)))
				.build();
	}

	/** 토큰이 만료·위조된 경우 필터가 남긴 사유(EXPIRED_TOKEN/INVALID_TOKEN)를, 없으면 UNAUTHORIZED 를 응답한다. */
	private AuthenticationEntryPoint entryPoint(ObjectMapper objectMapper) {
		return (request, response, e) -> {
			ErrorCode reason = JwtAuthenticationFilter.errorOf(request);
			write(objectMapper, response, reason != null ? reason : ErrorCode.UNAUTHORIZED);
		};
	}

	private AccessDeniedHandler accessDenied(ObjectMapper objectMapper) {
		return (request, response, e) -> write(objectMapper, response, ErrorCode.FORBIDDEN);
	}

	private void write(ObjectMapper objectMapper, HttpServletResponse response, ErrorCode errorCode)
			throws IOException {
		response.setStatus(errorCode.status().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
	}
}
