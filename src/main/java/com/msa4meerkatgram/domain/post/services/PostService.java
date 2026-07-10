package com.msa4meerkatgram.domain.post.services;


import com.msa4meerkatgram.domain.post.entities.Post;
import com.msa4meerkatgram.domain.post.repositories.PostQueryRepository;
import com.msa4meerkatgram.domain.post.repositories.PostRepository;
import com.msa4meerkatgram.domain.post.requests.PostIndexReq;
import com.msa4meerkatgram.domain.post.requests.PostStoreReq;
import com.msa4meerkatgram.domain.post.responses.PostIndexRes;
import com.msa4meerkatgram.domain.post.responses.PostWithUserRes;
import com.msa4meerkatgram.domain.user.repositories.UserRepository;
import com.msa4meerkatgram.global.errors.custom.DeletedRecordException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;
    private final UserRepository userRepository;

    public PostIndexRes index(PostIndexReq postIndexReq) {
        int offset = (postIndexReq.page() - 1) * postIndexReq.limit();

        // 특정 페이지 게시글 조회
        List<Post> result = postQueryRepository.pagination(offset, postIndexReq.limit());

        // 토탈 획득
        long total = postRepository.count();
        boolean lastPage = offset + postIndexReq.limit() >= total;

        // 컨트롤러 전달
        return PostIndexRes.from(total, lastPage, result);
    }

    public PostWithUserRes show(long id) {
        Post result = postRepository.findById(id)
            .orElseThrow(() -> new DeletedRecordException("이미 삭제된 게시글입니다."));

        return PostWithUserRes.from(result);
    }

    @Transactional(rollbackFor = Exception.class)
    public PostWithUserRes store(long userId, PostStoreReq postStoreReq) {
        // 작성 게시글 객체 생성
        Post post = new Post();
        post.setContent(postStoreReq.content());
        post.setImage(postStoreReq.image());
        post.setUser(userRepository.getReferenceById(userId));

        // 게시글 작성 처리
        // 새로 작성한 게시글 획득 및 반환
        return PostWithUserRes.from(postRepository.save(post));
    }
}
