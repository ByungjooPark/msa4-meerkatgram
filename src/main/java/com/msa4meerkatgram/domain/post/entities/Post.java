package com.msa4meerkatgram.domain.post.entities;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Post {
    private Long id;
    private Long userId;
    private String content;
    private String image;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
}
