package com.msa4meerkatgram.domain.auth.controllers;

import com.msa4meerkatgram.domain.auth.requests.LoginRequestDTO;
import com.msa4meerkatgram.domain.auth.requests.RegistrationRequestDTO;
import com.msa4meerkatgram.domain.auth.responses.AuthResponseDTO;
import com.msa4meerkatgram.domain.auth.services.AuthService;
import com.msa4meerkatgram.global.errors.custom.InvalidTokenException;
import com.msa4meerkatgram.global.responses.GlobalResponseDTO;
import com.msa4meerkatgram.global.cookie.CookieManager;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<GlobalResponseDTO<AuthResponseDTO>> login(
        @Valid @RequestBody LoginRequestDTO loginRequestDTO
        , HttpServletResponse response
    ) {
        AuthResponseDTO result = authService.login(loginRequestDTO);
        cookieManager.setRefreshTokenToCookie(response, result.refreshToken());

        return ResponseEntity.ok(GlobalResponseDTO.success(result));
    }

    @PostMapping("/reissue-token")
    public ResponseEntity<GlobalResponseDTO<AuthResponseDTO>> reissue(
        HttpServletRequest request
        ,HttpServletResponse response
    ) {
        // 쿠키에서 리프레시 토큰 획득
        String refreshToken = cookieManager.getRefreshTokenFromCookie(request)
            .orElseThrow(() -> new InvalidTokenException("토큰이 없습니다."));

        AuthResponseDTO result = authService.reissue(refreshToken);
        cookieManager.setRefreshTokenToCookie(response, result.refreshToken());

        return ResponseEntity.ok(GlobalResponseDTO.success(result));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<GlobalResponseDTO<Void>> logout(
        HttpServletResponse response
        , @AuthenticationPrincipal Claims claims
    ) {
        authService.logout(Long.parseLong(claims.getSubject()));
        cookieManager.removeRefreshTokenToCookie(response);

        return ResponseEntity.ok(GlobalResponseDTO.success());
    }

    @PostMapping("/registration")
    public ResponseEntity<GlobalResponseDTO<Void>> registration(
        @Valid @RequestBody RegistrationRequestDTO registrationRequestDTO
        ) {
        authService.registration(registrationRequestDTO);

        return ResponseEntity.ok(GlobalResponseDTO.success());
    }
}
