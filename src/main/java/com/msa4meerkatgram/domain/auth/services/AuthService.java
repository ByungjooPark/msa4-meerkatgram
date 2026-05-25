package com.msa4meerkatgram.domain.auth.services;

import com.msa4meerkatgram.domain.auth.mapper.AuthMapper;
import com.msa4meerkatgram.domain.auth.requests.LoginReq;
import com.msa4meerkatgram.domain.auth.responses.AuthRes;
import com.msa4meerkatgram.domain.user.entities.User;
import com.msa4meerkatgram.domain.user.mapper.UserMapper;
import com.msa4meerkatgram.domain.user.responses.UserRes;
import com.msa4meerkatgram.global.errors.custom.InvalidTokenException;
import com.msa4meerkatgram.global.errors.custom.NotRegisteredException;
import com.msa4meerkatgram.global.security.cookie.CookieManager;
import com.msa4meerkatgram.global.security.jwt.JwtConfig;
import com.msa4meerkatgram.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final AuthMapper authMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieManager cookieManager;

    @Transactional
    public AuthRes login(LoginReq loginRequest, HttpServletResponse response) {
        // 유저 획득
        User user = userMapper.findByEmail(loginRequest.email());

        // 유저 가입 여부 확인
        if(user == null) {
            throw new NotRegisteredException("아이디와 비밀번호를 확인해주세요.");
        }

        // 비밀번호 체크
        if(!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new NotRegisteredException("아이디와 비밀번호를 확인해주세요.");
        }

        return generateAuthentication(user, response);
    }

    @Transactional
    public AuthRes reissue(HttpServletRequest request, HttpServletResponse response) {
        // 리프래시 토큰 획득
        Optional<String> refreshTokenOptional = jwtTokenProvider.extractRefreshToken(request);
        if(refreshTokenOptional.isEmpty()) {
            throw new InvalidTokenException("토큰 미존재");
        }
        String refreshToken = refreshTokenOptional.get();

        // userId  획득
        long id = Long.parseLong(jwtTokenProvider.extractClaims(refreshToken).getSubject());

        // 유저 획득
        User user = userMapper.findByPk(id);

        // 유저 가입 여부 확인
        if(user == null) {
            throw new InvalidTokenException("유효하지 않은 회원의 토큰입니다.");
        }

        // 리프래시 토큰 비교
        if(!refreshToken.equals(user.getRefreshToken())) {
            throw new InvalidTokenException("토큰이 일치하지 않습니다.");
        }

        return generateAuthentication(user, response);
    }

    public void logout(HttpServletResponse response, long id) {
        // 유저 획득
        User user = userMapper.findByPk(id);

        if(user == null) {
            throw new InvalidTokenException("유효하지 않은 회원의 토큰입니다.");
        }

        // DB 갱신
        authMapper.updateRefreshToken(id, null);

        // 리프래시 토큰 쿠키 삭제
        cookieManager.setCookie(response, jwtConfig.refreshTokenCookieName(), null, 0, jwtConfig.reissUri());
    }

    private AuthRes generateAuthentication(User user, HttpServletResponse response) {
        // 토큰생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        // 리프래시 토큰 DB 저장
        authMapper.updateRefreshToken(user.getId(), newRefreshToken);

        // 리프래시 토큰 쿠키 저장
        cookieManager.setCookie(response, jwtConfig.refreshTokenCookieName(), newRefreshToken, jwtConfig.refreshTokenCookieExpiry(), jwtConfig.reissUri());

        return AuthRes.builder()
            .accessToken(newAccessToken)
            .user(UserRes.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nick(user.getNick())
                .role(user.getRole())
                .profile(user.getProfile())
                .createdAt(user.getCreatedAt())
                .build()
            )
            .build();
    }
}
