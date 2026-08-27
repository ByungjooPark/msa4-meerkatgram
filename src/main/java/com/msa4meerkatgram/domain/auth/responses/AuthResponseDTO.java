package com.msa4meerkatgram.domain.auth.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.msa4meerkatgram.domain.user.responses.UserResponseDTO;
import lombok.Builder;

@Builder
public record AuthResponseDTO(
    UserResponseDTO user,
    String accessToken,
    @JsonIgnore
    String refreshToken
) {
}
