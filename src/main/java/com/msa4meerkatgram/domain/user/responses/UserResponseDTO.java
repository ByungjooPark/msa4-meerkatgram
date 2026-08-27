package com.msa4meerkatgram.domain.user.responses;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserResponseDTO(
    long id
    , String email
    , String nick
    , String role
    , String profile
    , LocalDateTime createdAt
    , long countPosts
) {
}
