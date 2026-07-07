package com.msa4meerkatgram.domain.user.responses;

import com.msa4meerkatgram.domain.user.entities.User;

public record UserWithPostCountRes(
    UserRes user
    ,long countPosts
) {
    public static UserWithPostCountRes from(User user, long countPost) {
        return new UserWithPostCountRes(
            UserRes.from(user)
            , countPost
        );
    }
}
