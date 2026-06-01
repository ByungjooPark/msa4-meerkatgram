package com.msa4meerkatgram.global.security.filter;

import com.msa4meerkatgram.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityAuthenticationProvider {
    private final JwtProvider jwtProvider;

    // 스프링 시큐리티에서 사용자의 인증정보를 담는 객체를 생성
    public Authentication authentication(String token) {
        // 각 아규먼트는 인증 된 사용자 객체(Claims), 비밀번호 저장 여부, 사용자 권한 목록
        return new UsernamePasswordAuthenticationToken(
            jwtProvider.extractClaims(token)
            , null
            , List.of()
        );
    }
}
