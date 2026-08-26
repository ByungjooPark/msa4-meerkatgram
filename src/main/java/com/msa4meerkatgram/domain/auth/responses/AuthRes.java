package com.msa4meerkatgram.domain.auth.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.msa4meerkatgram.domain.user.responses.UserRes;
import lombok.Builder;

@Builder
public record AuthRes(
    UserRes user,
    String accessToken,
    @JsonIgnore
    String refreshToken
) {
}
