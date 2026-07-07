package com.msa4meerkatgram.domain.auth.responses;

import com.msa4meerkatgram.domain.user.entities.User;
import com.msa4meerkatgram.domain.user.responses.UserWithPostCountRes;

public record AuthRes(
    UserWithPostCountRes user
    ,String accessToken
) {
    public static AuthRes from(User user, String accessToken, long countPosts) {
        return new AuthRes(
            UserWithPostCountRes.from(user, countPosts)
            , accessToken
        );
    }
}
