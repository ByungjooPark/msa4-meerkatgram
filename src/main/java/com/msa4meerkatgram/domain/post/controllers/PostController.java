package com.msa4meerkatgram.domain.post.controllers;

import com.msa4meerkatgram.domain.post.entities.Post;
import com.msa4meerkatgram.domain.post.requests.PostCreateReq;
import com.msa4meerkatgram.domain.post.requests.PostIndexReq;
import com.msa4meerkatgram.domain.post.responses.PostIndexRes;
import com.msa4meerkatgram.domain.post.services.PostService;
import com.msa4meerkatgram.global.responses.GlobalRes;
import io.jsonwebtoken.Claims;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class PostController {
    private final PostService postService;

    @GetMapping("/posts")
    public ResponseEntity<GlobalRes<PostIndexRes>> index(PostIndexReq postIndexReq) {
        PostIndexRes postIndexRes = postService.index(postIndexReq);

        return ResponseEntity.status(200).body(
            GlobalRes.<PostIndexRes>builder()
                    .code("00")
                    .message("정상처리")
                    .data(postIndexRes)
                    .build()
        );
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<GlobalRes<Post>> show(
        @Min(value = 1, message = "1이상 숫자만 허용합니다.") @PathVariable Long id
    ) {
        Post result = postService.show(id);

        GlobalRes<Post> globalRes = GlobalRes.<Post>builder()
            .code("00")
            .message("정상 처리")
            .data(result)
            .build();

        return ResponseEntity.status(200).body(globalRes);
    }

    @PostMapping("/posts")
    public ResponseEntity<GlobalRes<Post>> store(
        @AuthenticationPrincipal Claims claims
        , @RequestBody PostCreateReq postCreateReq
    ) {
        Post result = postService.store(Long.parseLong(claims.getSubject()), postCreateReq);

        GlobalRes<Post> globalRes = GlobalRes.<Post>builder()
            .code("00")
            .message("정상 처리")
            .data(result)
            .build();

        return ResponseEntity.status(200).body(globalRes);
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<GlobalRes<String>> delete(
        @Min(value = 1, message = "1이상 숫자만 허용합니다.") @PathVariable Long id
    ) {
        postService.destroy(id);

        GlobalRes<String> globalRes = GlobalRes.<String>builder()
            .code("00")
            .message("정상 처리")
            .build();

        return ResponseEntity.status(200).body(globalRes);
    }
}
