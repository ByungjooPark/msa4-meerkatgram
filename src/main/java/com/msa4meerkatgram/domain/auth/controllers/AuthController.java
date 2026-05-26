package com.msa4meerkatgram.domain.auth.controllers;

import com.msa4meerkatgram.domain.auth.requests.LoginReq;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<GlobalRes<AuthRes>> login(
        @Valid @RequestBody LoginReq LoginReq
        , HttpServletResponse response
    ) {
        AuthRes result = authService.login(LoginReq, response);

        GlobalRes<AuthRes> globalRes = GlobalRes.<AuthRes>builder()
            .code("00")
            .message("정상 처리")
            .data(result)
            .build();

        return ResponseEntity.status(200).body(globalRes);
    }

    @PostMapping("/reissue-token")
    public ResponseEntity<GlobalRes<AuthRes>> reissue(
        HttpServletRequest request
        , HttpServletResponse response
    ) {
        AuthRes result = authService.reissue(request, response);

        GlobalRes<AuthRes> globalRes = GlobalRes.<AuthRes>builder()
            .code("00")
            .message("정상 처리")
            .data(result)
            .build();

        return ResponseEntity.status(200).body(globalRes);
    }

    @PostMapping("/logout")
    public ResponseEntity<GlobalRes<String>> logout(
        @AuthenticationPrincipal Claims claims
        , HttpServletResponse response
    ) {
        authService.logout(response, Long.parseLong(claims.getSubject()));

        GlobalRes<String> globalRes = GlobalRes.<String>builder()
            .code("00")
            .message("정상 처리")
            .build();

        return ResponseEntity.status(200).body(globalRes);
    }
}
