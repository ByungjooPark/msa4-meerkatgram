package com.msa4meerkatgram.domain.post.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

public record PostIndexReq(
    @Schema(description = "페이지 번호", example = "1", nullable = false, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Min(value = 1,  message = "1이상 숫자만 허용합니다.")
    Integer page,
    @Schema(description = "게시글 제한 수", example = "6", nullable = false, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Min(value = 1,  message = "1이상 숫자만 허용합니다.")
    Integer limit
) {
    public PostIndexReq(Integer page, Integer limit) {
        this.page = (page != null && page > 0) ? page : 1;
        this.limit = (limit != null && limit > 0) ? limit : 6;
    }
}
