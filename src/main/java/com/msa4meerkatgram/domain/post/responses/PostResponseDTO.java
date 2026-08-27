package com.msa4meerkatgram.domain.post.responses;

import com.msa4meerkatgram.domain.post.entities.Post;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * ** 직접사용 금지 **
 * Image URL의 도메인 합성을 위해 반드시 PostResponseMapper를 통해서만 사용할것
 * @param id
 * @param userId
 * @param content
 * @param image
 * @param createdAt
 * @param updatedAt
 */
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
