package com.msa4meerkatgram.domain.auth.controllers;

import com.msa4meerkatgram.domain.auth.requests.LoginReq;
import com.msa4meerkatgram.domain.auth.requests.RegistrationReq;
import com.msa4meerkatgram.domain.auth.responses.AuthRes;
import com.msa4meerkatgram.domain.auth.services.AuthService;
import com.msa4meerkatgram.global.errors.custom.InvalidTokenException;
import com.msa4meerkatgram.global.responses.GlobalRes;
import com.msa4meerkatgram.global.security.cookie.CookieManager;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;
    private final CookieManager cookieManager;

    @PostMapping("/login")
    public ResponseEntity<GlobalRes<AuthRes>> login(
        @Valid @RequestBody LoginReq loginReq
        , HttpServletResponse response
    ) {
        AuthRes result = authService.login(loginReq);
        cookieManager.setRefreshTokenToCookie(response, result.refreshToken());

        return ResponseEntity.ok(GlobalRes.success(result));
    }

    @PostMapping("/reissue-token")
    public ResponseEntity<GlobalRes<AuthRes>> reissue(
        HttpServletRequest request
        ,HttpServletResponse response
    ) {
        // 쿠키에서 리프레시 토큰 획득
        String refreshToken = cookieManager.getRefreshTokenFromCookie(request)
            .orElseThrow(() -> new InvalidTokenException("토큰이 없습니다."));

        AuthRes result = authService.reissue(refreshToken);
        cookieManager.setRefreshTokenToCookie(response, result.refreshToken());

        return ResponseEntity.ok(GlobalRes.success(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<GlobalRes<Void>> logout(
        HttpServletResponse response
        , @AuthenticationPrincipal Claims claims
    ) {
        authService.logout(Long.parseLong(claims.getSubject()));
        cookieManager.removeRefreshTokenToCookie(response);

        return ResponseEntity.ok(GlobalRes.success());
    }

    @PostMapping("/registration")
    public ResponseEntity<GlobalRes<Void>> registration(
        @Valid @RequestBody RegistrationReq registrationReq
        ) {
        authService.registration(registrationReq);

        return ResponseEntity.ok(GlobalRes.success());
    }
}
