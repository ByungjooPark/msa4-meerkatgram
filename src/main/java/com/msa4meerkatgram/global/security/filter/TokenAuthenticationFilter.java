package com.msa4meerkatgram.global.security.filter;

import com.msa4meerkatgram.global.security.jwt.JwtProvider;
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

    private final JwtProvider jwtProvider;
    private final SecurityAuthenticationProvider securityAuthenticationProvider;
    private final HandlerExceptionResolver handlerExceptionResolver;

    // 엑세스 토큰의 유효 여부를 확인하고, 인증 정보를 스프링 시큐리티에 설정하는 메소드
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 헤더에서 엑세스 토큰
        Optional<String> tokenOptional = jwtProvider.extractAccessToken(request);

        // 토큰이 존재할 때만 인증 로직 실행
        if(tokenOptional.isPresent()) {
            try {
                // Security 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(securityAuthenticationProvider.authentication(tokenOptional.get()));
            } catch (Exception e) {
                // 예외를 핸들러 리졸버로 위임(@RestControllerAdvice가 처리하도록 됨)
                handlerExceptionResolver.resolveException(request, response, null, e);
                return; // 예외 위임 응답 완료 후 필터 체인을 중단하기위해 return
            }
        }

        // 다음 필터 호출
        filterChain.doFilter(request, response);
    }
}
