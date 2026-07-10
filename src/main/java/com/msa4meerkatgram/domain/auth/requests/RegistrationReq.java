package com.msa4meerkatgram.domain.auth.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "회원가입 시 필요 데이터")
public record RegistrationReq(
    @Schema(description = "이메일", example = "test4@test.com", nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이메일은 필수 항목입니다.")
    @Pattern(regexp = "^[0-9a-zA-Z](?!.*?[\\-_.]{2})[a-zA-Z0-9\\-_.]{3,63}@[0-9a-zA-Z](?!.*?[\\-_.]{2})[a-zA-Z0-9\\-_.]{3,63}\\.[a-zA-Z]{2,3}$", message = "허용하지 않는 이메일 양식입니다.")
    String email,

    @Schema(description = "비밀번호", example = "qwer1234", nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호는 필수 항목입니다.")
    @Pattern(regexp = "^[0-9a-zA-Z!@#$%^&*()]{8,20}$", message = "허용하지 않는 비밀번호 양식입니다.")
    String password,

    @Schema(description = "비밀번호 확인", example = "qwer1234", nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호 확인은 필수 항목입니다.")
    String passwordChk,

    @Schema(description = "닉네임", example = "Kanna_Kamui", nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "닉네임은 필수 항목입니다.")
    @Pattern(regexp = "^[0-9a-zA-Z_]{2,20}$", message = "허용하지 않는 닉네임 양식입니다.")
    String nick,

    @Schema(description = "프로필", example = "http://localhost:8080/files/profiles/4e98w6gy3498f52.png", nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "프로필은 필수 항목입니다.")
    String profile
) {
    @Schema(hidden = true)
    @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordMatch() {
        if(this.password == null || this.passwordChk == null) {
            return false;
        }
        return this.password.equals(this.passwordChk);
    }
}
