package com.msa4meerkatgram.domain.post.responses;

import com.msa4meerkatgram.domain.post.entities.Post;
import lombok.Builder;

import java.util.List;

@Builder
public record PostIndexRes(
        long total
        ,boolean lastPage
        ,List<Post> posts
) {
}
