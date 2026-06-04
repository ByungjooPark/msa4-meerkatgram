package com.msa4meerkatgram.domain.auth.controllers;

import com.msa4meerkatgram.domain.auth.requests.LoginReq;
import com.msa4meerkatgram.domain.auth.requests.RegistrationReq;
import com.msa4meerkatgram.domain.auth.responses.AuthRes;
import com.msa4meerkatgram.domain.auth.services.AuthService;
import com.msa4meerkatgram.global.responses.GlobalRes;
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

    @PostMapping("/login")
    public ResponseEntity<GlobalRes<AuthRes>> login(
        @Valid @RequestBody LoginReq loginReq
        , HttpServletResponse response
    ) {
        return ResponseEntity.status(200).body(
            GlobalRes.<AuthRes>builder()
                .code("00")
                .message("로그인 완료")
                .data(authService.login(response, loginReq))
                .build()
        );
    }

    @PostMapping("/reissue-token")
    public ResponseEntity<GlobalRes<AuthRes>> reissue(
        HttpServletRequest request
        ,HttpServletResponse response
    ) {
        return ResponseEntity.status(200).body(
            GlobalRes.<AuthRes>builder()
                .code("00")
                .message("토큰 재발급 완료")
                .data(authService.reissue(request, response))
                .build()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<GlobalRes<String>> logout(
        HttpServletResponse response
        , @AuthenticationPrincipal Claims claims
    ) {
        authService.logout(response, Long.parseLong(claims.getSubject()));

        return ResponseEntity.status(200).body(
            GlobalRes.<String>builder()
                .code("00")
                .message("로그아웃 완료")
                .build()
        );
    }

    @PostMapping("/registration")
    public ResponseEntity<GlobalRes<String>> registration(
        @Valid @RequestBody RegistrationReq registrationReq
        ) {
        authService.registration(registrationReq);

        return ResponseEntity.status(200).body(
            GlobalRes.<String>builder()
                .code("00")
                .message("회원가입 완료")
                .build()
        );
    }
}
