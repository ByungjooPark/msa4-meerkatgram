package com.msa4meerkatgram.domain.auth.controllers;

import com.msa4meerkatgram.domain.auth.requests.LoginReq;
import com.msa4meerkatgram.domain.auth.requests.RegistrationReq;
import com.msa4meerkatgram.domain.auth.responses.AuthRes;
import com.msa4meerkatgram.domain.auth.services.AuthService;
import com.msa4meerkatgram.global.annotations.openapi.ApiNotValidErrorResponse;
import com.msa4meerkatgram.global.responses.GlobalRes;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

// @Tag: API들을 기능별 또는 도메인별로 그룹화 할때 사용
@Tag(name = "인증 API", description = "인증 및 인가 담당")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "로그인 처리", description = "이메일과 비밀번호로 로그인")
    @ApiResponse(responseCode = "200", description = "로그인 성공")
    @ApiNotValidErrorResponse
    @PostMapping("/login")
    public ResponseEntity<GlobalRes<AuthRes>> login(
        @Valid @RequestBody LoginReq loginReq
        , HttpServletResponse response
    ) {
        return ResponseEntity.ok(GlobalRes.success(authService.login(response, loginReq)));
    }

    @PostMapping("/reissue-token")
    public ResponseEntity<GlobalRes<AuthRes>> reissue(
        HttpServletRequest request
        ,HttpServletResponse response
    ) {
        return ResponseEntity.ok(GlobalRes.success(authService.reissue(request, response)));
    }

    @PostMapping("/logout")
    public ResponseEntity<GlobalRes<Void>> logout(
        HttpServletResponse response
        , @AuthenticationPrincipal Claims claims
    ) {
        authService.logout(response, Long.parseLong(claims.getSubject()));

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
