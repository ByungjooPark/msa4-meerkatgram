package com.msa4meerkatgram.domain.user.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

@Builder
public record RegistrationReq(
        @NotBlank(message = "필수항목입니다.")
        String email,

        @NotBlank(message = "필수항목입니다.")
        String password,

        @NotBlank(message = "필수항목입니다.")
        String nick,

        @NotNull(message = "필수항목입니다.")
        MultipartFile profile
) {
}