package com.msa4meerkatgram.domain.user.responses;

import com.msa4meerkatgram.domain.user.entities.User;
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
    public static UserResponseDTO from(User user, long countPosts) {
        return new UserResponseDTO(
            user.getId(),
            user.getEmail(),
            user.getNick(),
            user.getRole(),
            user.getProfile(),
            user.getCreatedAt(),
            countPosts
        );
    }
}
