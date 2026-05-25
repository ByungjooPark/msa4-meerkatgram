package com.msa4meerkatgram.global.security.cookie;

import com.msa4meerkatgram.global.security.jwt.JwtConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieManager {
    private final JwtConfig jwtConfig;

    public CookieManager(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    // Request Header에서 특정 쿠키를 획득 (Optional 반환)
    public Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(name))
                .findFirst();
    }

    // 기본 경로("/")를 사용하는 편리한 쿠키 생성 메소드 (오버로딩)
    public void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
        setCookie(response, name, value, maxAge, "/");
    }

    // 상세 설정을 포함한 쿠키 생성 메소드
    public void setCookie(HttpServletResponse response, String name, String value, int maxAge, String path) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(true);               // XSS 공격 방지
        cookie.setSecure(jwtConfig.secure());   // MITM 공격 방지 (HTTPS 환경 설정 주입)

        response.addCookie(cookie);
    }
}
