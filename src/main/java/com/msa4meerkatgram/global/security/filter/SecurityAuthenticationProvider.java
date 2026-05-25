package com.msa4meerkatgram.global.security.filter;

import com.msa4meerkatgram.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityAuthenticationProvider {

    private final JwtTokenProvider jwtTokenProvider;

    // 스프링시큐리티에서 사용자의 인증정보를 담는 객체 생성
    public Authentication authentication(String token) {
        // 각 파라미터는 인증 된 사용자 객체, 비밀번호 저장여부, 사용자 권한 목록
        return new UsernamePasswordAuthenticationToken(jwtTokenProvider.extractClaims(token), null, List.of());
    }
}
