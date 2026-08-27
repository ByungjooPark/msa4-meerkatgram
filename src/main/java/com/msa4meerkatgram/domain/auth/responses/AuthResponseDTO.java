package com.msa4meerkatgram.domain.auth.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.msa4meerkatgram.domain.user.entities.User;
import com.msa4meerkatgram.domain.user.responses.UserResponseDTO;
import lombok.Builder;

@Builder
public record AuthResponseDTO(
    UserResponseDTO user,
    String accessToken,
    @JsonIgnore
    String refreshToken
) {
    public static AuthResponseDTO from(User user, String accessToken, String refreshToken, long countPosts) {
        return new AuthResponseDTO(
            UserResponseDTO.from(user, countPosts),
            accessToken,
            refreshToken
        );
    }
}
