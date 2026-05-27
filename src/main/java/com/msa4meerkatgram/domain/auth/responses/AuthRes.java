package com.msa4meerkatgram.domain.auth.responses;

import com.msa4meerkatgram.domain.user.entities.User;
import lombok.Builder;

@Builder
public record AuthRes(
    User user
    ,String accessToken
) {

}
