package com.msa4meerkatgram.global.security.jwt;

import com.msa4meerkatgram.domain.user.entities.User;
import com.msa4meerkatgram.global.errors.custom.InvalidTokenException;
import com.msa4meerkatgram.global.security.cookie.CookieManager;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtTokenProvider {
    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;
    private final CookieManager cookieManager;

    public JwtTokenProvider(JwtConfig jwtConfig, CookieManager cookieManager) {
        this.jwtConfig = jwtConfig;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtConfig.secret()));
        this.cookieManager = cookieManager;
    }

    public String generateAccessToken(User user) {
        return generateToken(user, jwtConfig.accessTokenExpiry());
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, jwtConfig.refreshTokenExpiry());
    }

    private String generateToken(User user, long ttl) {
        Date now = new Date();
        return Jwts.builder()
                .header().type(jwtConfig.type())
                .and()
                .subject(String.valueOf(user.getId()))
                .issuer(jwtConfig.issuer())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl))
                .claim("role", user.getRole())
                .signWith(this.secretKey)
                .compact();
    }

    // 헤더에서 엑세스 토큰 문자열만 순수하게 추출
    public Optional<String> extractAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtConfig.headerKey());

        if (bearerToken == null || !bearerToken.startsWith(jwtConfig.scheme())) {
            return Optional.empty();
        }

        return Optional.of(bearerToken.substring(jwtConfig.scheme().length() + 1));
    }

    // 쿠키에서 리프레시 토큰 추출 (Stream 코드 가독성 개선)
    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        return cookieManager.getCookie(request, jwtConfig.refreshTokenCookieName())
                .map(Cookie::getValue);
    }

    // 토큰 검증 및 클레임 추출
    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(this.secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("토큰이 만료되었습니다.");
        } catch (UnsupportedJwtException e) {
            throw new InvalidTokenException("서명이 위조된 유효하지 않은 토큰입니다.");
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException("토큰 형식이 올바르지 않습니다.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("인증 토큰 검증에 실패했습니다.");
        }
    }
}
