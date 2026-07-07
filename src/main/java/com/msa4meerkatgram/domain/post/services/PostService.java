package com.msa4meerkatgram.domain.post.services;

import com.msa4meerkatgram.domain.post.entities.Post;
import com.msa4meerkatgram.domain.post.repositories.PostRepository;
import com.msa4meerkatgram.domain.post.responses.PostWithUserRes;
import com.msa4meerkatgram.global.errors.custom.DeletedRecordException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    // public PostIndexRes index(PostIndexReq postIndexReq) {
    //     int offset = (postIndexReq.page() - 1) * postIndexReq.limit();
    //
    //     // 특정 페이지 게시글 조회
    //     List<PostMybatis> posts = postMapper.getPagination(postIndexReq.limit(), offset);
    //
    //     // 토탈 획득
    //     long total = postMapper.getTotal();
    //     boolean lastPage = offset + postIndexReq.limit() >= total;
    //
    //     // 컨트롤러 전달
    //     return PostIndexRes.builder()
    //             .total(total)
    //             .lastPage(lastPage)
    //             .posts(posts)
    //             .build();
    // }

    public PostWithUserRes show(long id) {
        Post result = postRepository.findById(id)
            .orElseThrow(() -> new DeletedRecordException("이미 삭제된 게시글입니다."));

        return PostWithUserRes.from(result);
    }
}
