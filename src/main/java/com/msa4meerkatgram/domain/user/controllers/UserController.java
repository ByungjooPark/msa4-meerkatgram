package com.msa4meerkatgram.domain.user.controllers;

import com.msa4meerkatgram.domain.user.requests.RegistrationReq;
import com.msa4meerkatgram.domain.user.responses.UserRes;
import com.msa4meerkatgram.domain.user.services.UserService;
import com.msa4meerkatgram.global.responses.GlobalRes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {
    private final UserService userService;

    @GetMapping("/users/{id}")
    public ResponseEntity<GlobalRes<UserRes>> show(
        @Min(value = 1, message = "1이상 숫자만 허용합니다.") @PathVariable Long id
    ) {
        UserRes result = userService.show(id);

        GlobalRes<UserRes> globalRes = GlobalRes.<UserRes>builder()
            .code("00")
            .message("정상 처리")
            .data(result)
            .build();

        return ResponseEntity.status(200).body(globalRes);
    }

    @PostMapping("/users")
    public ResponseEntity<GlobalRes<UserRes>> create(
        @Valid @ModelAttribute RegistrationReq registrationRequestDTO
    ) {
        UserRes result = userService.create(registrationRequestDTO);

        GlobalRes<UserRes> globalRes = GlobalRes.<UserRes>builder()
            .code("00")
            .message("정상 처리")
            .data(result)
            .build();

        return ResponseEntity.status(200).body(globalRes);
    }
}
