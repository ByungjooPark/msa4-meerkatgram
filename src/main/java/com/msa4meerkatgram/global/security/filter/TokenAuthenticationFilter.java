package com.msa4meerkatgram.global.security.filter;

import com.msa4meerkatgram.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final SecurityAuthenticationProvider securityAuthenticationProvider;
    private final JwtTokenProvider jwtTokenProvider;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    // 엑세스 토큰의 유효 여부를 확인하고 인증 정보를 스프링 시큐리티에 설정하는 메소드
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 쿠키에서 토큰 획득
        Optional<String> tokenOptional = jwtTokenProvider.extractAccessToken(request);

        // 토큰이 존재할 때만 인증 로직 실행
        if (tokenOptional.isPresent()) {
            try {
                // Security 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(securityAuthenticationProvider.authentication(tokenOptional.get()));
            } catch (Exception e) {
                // 예외를 핸들러 리졸버로 위임하여 @RestControllerAdvice가 처리하게 함
                handlerExceptionResolver.resolveException(request, response, null, e);
                return; // 예외 위임 응답 완료 후 필터 체인 중단 (안하면 filterChain.doFilter()가 두번 실행 되서 응답 중복 됨)
            }
        }

        filterChain.doFilter(request, response); //다음 필터 호출
    }
}
