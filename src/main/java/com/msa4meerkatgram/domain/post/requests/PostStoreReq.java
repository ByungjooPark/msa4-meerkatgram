package com.msa4meerkatgram.domain.post.requests;

import io.swagger.v3.oas.annotations.media.Schema;

public record PostStoreReq(
    @Schema(description = "게시글 내용", example = "내용입니다.", nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
    String content,
    @Schema(description = "게시글 이미지 패스", example = "http://localhost:8080/files/posts/4e98w6gy3498f52.png", nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
    String image
) {
}
