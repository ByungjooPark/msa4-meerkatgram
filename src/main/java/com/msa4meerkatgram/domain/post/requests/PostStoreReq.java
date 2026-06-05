package com.msa4meerkatgram.domain.post.requests;

public record PostStoreReq(
    String content,
    String image
) {
}
