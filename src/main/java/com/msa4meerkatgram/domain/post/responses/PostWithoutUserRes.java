package com.msa4meerkatgram.domain.post.responses;

import com.msa4meerkatgram.domain.post.entities.Post;

import java.time.LocalDateTime;

public record PostWithoutUserRes(
        long id
        , String content
        , String image
        , LocalDateTime createdAt
        , LocalDateTime updatedAt
        , LocalDateTime deletedAt
) {
    public static PostWithoutUserRes from(Post post) {
        return new PostWithoutUserRes(
                post.getId()
                ,post.getContent()
                ,post.getImage()
                ,post.getCreatedAt()
                ,post.getUpdatedAt()
                ,post.getDeletedAt()
        );
    }
}
