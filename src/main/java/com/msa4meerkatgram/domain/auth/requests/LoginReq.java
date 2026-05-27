package com.msa4meerkatgram.domain.auth.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginReq(
    @NotBlank(message = "이메일은 필수입니다.")
    @Pattern(regexp = "^[0-9a-zA-Z](?!.*?[\\-_.]{2})[a-zA-Z0-9\\-_.]{3,63}@[0-9a-zA-Z](?!.*?[\\-_.]{2})[a-zA-Z0-9\\-_.]{3,63}\\.[a-zA-Z]{2,3}$", message = "허용하지 않는 양식입니다.")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(regexp = "^[0-9a-zA-Z!@#$%^&*()]{8,20}$", message = "허용하지 않는 양식입니다.")
    String password
) {
}
