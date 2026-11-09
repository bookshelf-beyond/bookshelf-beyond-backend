package com.bookswap.auth.jwt;

import com.bookswap.global.error.BusinessException;
import com.bookswap.global.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization: Bearer 토큰을 검증해 SecurityContext 를 채운다. principal 은 회원 ID(Long).
 * 토큰이 잘못돼도 여기서 응답을 쓰지 않는다. 인증을 비워 두고 사유만 요청 속성에 남기면,
 * 인증이 필요한 경로에서 AuthenticationEntryPoint 가 401 을 쓴다. (공개 경로는 그대로 통과)
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	public static final String ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".ERROR";

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider tokenProvider;

	public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
		this.tokenProvider = tokenProvider;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			authenticate(request, header.substring(BEARER_PREFIX.length()).trim());
		}
		chain.doFilter(request, response);
	}

	private void authenticate(HttpServletRequest request, String token) {
		try {
			long memberId = tokenProvider.parseMemberId(token);
			var authentication = new UsernamePasswordAuthenticationToken(
					memberId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (BusinessException e) {
			request.setAttribute(ERROR_ATTRIBUTE, e.errorCode());
		}
	}

	public static ErrorCode errorOf(HttpServletRequest request) {
		return request.getAttribute(ERROR_ATTRIBUTE) instanceof ErrorCode code ? code : null;
	}
}
