package com.msa4meerkatgram.domain.post.responses;

import com.msa4meerkatgram.domain.post.entities.Post;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PostResponseDTO(
    Long id,
    Long userId,
    String content,
    String image,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static PostResponseDTO from(Post post) {
        return new PostResponseDTO(
            post.getId(),
            post.getUserId(),
            post.getContent(),
            post.getImage(),
            post.getCreatedAt(),
            post.getUpdatedAt()
        );
    }
}
