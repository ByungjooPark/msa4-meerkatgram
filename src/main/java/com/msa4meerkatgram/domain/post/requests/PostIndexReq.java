package com.msa4meerkatgram.domain.post.requests;

import jakarta.validation.constraints.Min;

public record PostIndexReq(
        @Min(value = 1,  message = "1이상 숫자만 허용합니다.")
        Integer page,
        @Min(value = 1,  message = "1이상 숫자만 허용합니다.")
        Integer limit
) {
    public PostIndexReq(Integer page, Integer limit) {
        this.page = (page != null && page > 0) ? page : 1;
        this.limit = (limit != null && limit > 0) ? limit : 6;
    }
}
