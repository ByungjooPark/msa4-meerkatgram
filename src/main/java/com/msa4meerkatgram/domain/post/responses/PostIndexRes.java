package com.msa4meerkatgram.domain.post.responses;

import com.msa4meerkatgram.domain.post.entities.Post;

import java.util.List;

public record PostIndexRes(
        long total
        ,boolean lastPage
        ,List<PostWithUserRes> posts
) {
    public static PostIndexRes from(long total, boolean lastPage, List<Post> posts) {
        return new PostIndexRes(
            total
            , lastPage
            , posts.stream().map(PostWithUserRes::from).toList()
        );
    }
}
