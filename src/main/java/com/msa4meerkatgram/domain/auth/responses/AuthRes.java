package com.msa4meerkatgram.domain.auth.responses;

import com.msa4meerkatgram.domain.user.entities.User;
import com.msa4meerkatgram.domain.user.responses.UserWithPostCountRes;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 레스폰스")
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
