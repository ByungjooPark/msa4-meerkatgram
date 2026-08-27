package com.msa4meerkatgram.domain.post.responses;

import com.msa4meerkatgram.domain.post.entities.Post;
import lombok.Builder;

import java.util.List;

@Builder
public record PostIndexResponseDTO(
    long total
    , boolean isLastPage
    , List<PostResponseDTO> posts
) {
    public static PostIndexResponseDTO from(long total, boolean isLastPage, List<Post> posts) {
        return new PostIndexResponseDTO(
            total,
            isLastPage,
            posts.stream().map(PostResponseDTO::from).toList()
        );
    }
}
