package com.msa4meerkatgram.domain.post.controllers;

import com.msa4meerkatgram.domain.post.entities.Post;
import com.msa4meerkatgram.domain.post.requests.PostIndexRequestDTO;
import com.msa4meerkatgram.domain.post.requests.PostStoreRequestDTO;
import com.msa4meerkatgram.domain.post.responses.PostIndexResponseDTO;
import com.msa4meerkatgram.domain.post.services.PostService;
import com.msa4meerkatgram.global.responses.GlobalResponseDTO;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    @GetMapping
    public ResponseEntity<GlobalResponseDTO<PostIndexResponseDTO>> index(PostIndexRequestDTO postIndexRequestDTO) {
        return ResponseEntity.ok(GlobalResponseDTO.success(postService.index(postIndexRequestDTO)));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<GlobalResponseDTO<Post>> show(
        @Min(value = 1, message = "1이상 숫자만 허용합니다.") @PathVariable long id
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(postService.show(id)));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<GlobalResponseDTO<Post>> store(
        @Valid @RequestBody PostStoreRequestDTO postStoreRequestDTO
        , @AuthenticationPrincipal Claims claims
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(postService.store(Long.parseLong(claims.getSubject()), postStoreRequestDTO)));
    }

    @PreAuthorize("hasRole('SUPER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalResponseDTO<Void>> destroy(
        @Min(value = 1, message = "1이상 숫자만 허용합니다.") @PathVariable Long id,
        @AuthenticationPrincipal Claims claims
    ) {
        long userId = Long.parseLong(claims.getSubject());
        postService.destroy(id, userId);

        return ResponseEntity.ok(GlobalResponseDTO.success());
    }
}
