package com.msa4meerkatgram.domain.post.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record PostCreateReq(
    @NotBlank(message = "필수항목입니다.")
    String content,
    @NotNull(message = "필수항목입니다.")
    MultipartFile image
) {
}
