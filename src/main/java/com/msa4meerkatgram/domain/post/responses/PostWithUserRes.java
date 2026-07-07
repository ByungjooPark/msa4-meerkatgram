package com.msa4meerkatgram.domain.post.responses;

import com.msa4meerkatgram.domain.post.entities.Post;
import com.msa4meerkatgram.domain.user.entities.User;

import java.time.LocalDateTime;

public record PostWithUserRes(
    Long id
    , String content
    , String image
    , LocalDateTime createAt
    , LocalDateTime updatedAt
    , LocalDateTime deletedAt
    , User user
) {
    public static PostWithUserRes from(Post post) {
        return new PostWithUserRes(
            post.getId()
            ,post.getContent()
            ,post.getImage()
            ,post.getCreatedAt()
            ,post.getUpdatedAt()
            ,post.getDeletedAt()
            ,post.getUser()
        );
    }
}
