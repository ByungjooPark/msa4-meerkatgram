package com.msa4meerkatgram.domain.auth.requests;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegistrationReq(
    @NotBlank(message = "이메일은 필수 항목입니다.")
    @Pattern(regexp = "^[0-9a-zA-Z](?!.*?[\\-_.]{2})[a-zA-Z0-9\\-_.]{3,63}@[0-9a-zA-Z](?!.*?[\\-_.]{2})[a-zA-Z0-9\\-_.]{3,63}\\.[a-zA-Z]{2,3}$", message = "허용하지 않는 이메일 양식입니다.")
    String email,

    @NotBlank(message = "비밀번호는 필수 항목입니다.")
    @Pattern(regexp = "^[0-9a-zA-Z!@#$%^&*()]{8,20}$", message = "허용하지 않는 비밀번호 양식입니다.")
    String password,

    @NotBlank(message = "비밀번호 체크는 필수 항목입니다.")
    String passwordChk,

    @NotBlank(message = "닉네임은 필수 항목입니다.")
    @Pattern(regexp = "^[0-9a-zA-Z_]{2,20}$", message = "허용하지 않는 닉네임 양식입니다.")
    String nick,

    @NotBlank(message = "프로필은 필수 항목입니다.")
    String profile
) {
    @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordMatch() {
        if(this.password == null || this.passwordChk == null) {
            return false;
        }
        return this.password.equals(this.passwordChk);
    }
}
