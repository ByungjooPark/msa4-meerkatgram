package com.msa4meerkatgram.domain.post.controllers;

import com.msa4meerkatgram.domain.post.entities.Post;
import com.msa4meerkatgram.domain.post.requests.PostIndexReq;
import com.msa4meerkatgram.domain.post.requests.PostStoreReq;
import com.msa4meerkatgram.domain.post.responses.PostIndexRes;
import com.msa4meerkatgram.domain.post.services.PostService;
import com.msa4meerkatgram.global.responses.GlobalRes;
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
@RequestMapping("/api")
public class PostController {
    private final PostService postService;

    @GetMapping("/posts")
    public ResponseEntity<GlobalRes<PostIndexRes>> index(PostIndexReq postIndexReq) {
        return ResponseEntity.ok(GlobalRes.success(postService.index(postIndexReq)));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/posts/{id}")
    public ResponseEntity<GlobalRes<Post>> show(
        @Min(value = 1, message = "1이상 숫자만 허용합니다.") @PathVariable long id
    ) {
        return ResponseEntity.ok(GlobalRes.success(postService.show(id)));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/posts")
    public ResponseEntity<GlobalRes<Post>> store(
        @Valid @RequestBody PostStoreReq postStoreReq
        , @AuthenticationPrincipal Claims claims
    ) {
        return ResponseEntity.ok(GlobalRes.success(postService.store(Long.parseLong(claims.getSubject()), postStoreReq)));
    }
}
