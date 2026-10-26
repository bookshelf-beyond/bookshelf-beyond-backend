package com.bookswap.global.config;

import com.bookswap.global.error.ErrorCode;
import com.bookswap.global.error.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/api/auth/**").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(entryPoint(objectMapper, ErrorCode.UNAUTHORIZED))
						.accessDeniedHandler(accessDenied(objectMapper)))
				.build();
	}

	private AuthenticationEntryPoint entryPoint(ObjectMapper objectMapper, ErrorCode errorCode) {
		return (request, response, e) -> write(objectMapper, response, errorCode);
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
