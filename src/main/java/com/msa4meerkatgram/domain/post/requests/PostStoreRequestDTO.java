package com.msa4meerkatgram.domain.post.requests;

public record PostStoreRequestDTO(
    String content,
    String image
) {
}
