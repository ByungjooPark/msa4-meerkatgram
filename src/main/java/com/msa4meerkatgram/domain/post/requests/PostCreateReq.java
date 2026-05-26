package com.msa4meerkatgram.domain.post.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostCreateReq(
    @NotBlank(message = "필수항목입니다.")
    String content,
    @NotNull(message = "필수항목입니다.")
    String image
) {
}
