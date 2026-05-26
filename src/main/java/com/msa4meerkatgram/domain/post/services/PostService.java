package com.msa4meerkatgram.domain.post.services;

import com.msa4meerkatgram.domain.post.entities.Post;
import com.msa4meerkatgram.domain.post.mapper.PostMapper;
import com.msa4meerkatgram.domain.post.requests.PostCreateReq;
import com.msa4meerkatgram.domain.post.requests.PostIndexReq;
import com.msa4meerkatgram.domain.post.responses.PostIndexRes;
import com.msa4meerkatgram.global.util.file.LocalFileManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostMapper postMapper;
    private final LocalFileManager localFileManager;

    public PostIndexRes index(PostIndexReq postIndexReq) {
        int offset = (postIndexReq.page() - 1) * postIndexReq.limit();

        // 특정 페이지 게시글 조회
        List<Post> posts = postMapper.getPagination(postIndexReq.limit(), offset);

        // 토탈 획득
        long total = postMapper.getTotal();
        boolean lastPage = offset + postIndexReq.limit() >= total;

        // 컨트롤러 전달
        return PostIndexRes.builder()
                .total(total)
                .lastPage(lastPage)
                .posts(posts)
                .build();
    }

    public Post show(long id) {
        Post result = postMapper.findByPk(id);

        if(result == null) {
            throw new RuntimeException("삭제된 게시글입니다.");
        }

        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Post store(long userId, PostCreateReq postCreateReq) {
        // DB 저장
        Post post = Post.builder()
            .userId(userId)
            .content(postCreateReq.content())
            .image(postCreateReq.image())
            .build();
        postMapper.create(post);

        // 게시글 정보 획득
        return postMapper.findByPk(post.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void destroy(long id) {
        // 게시글 정보 획득
        Post post = postMapper.findByPk(id);
        if (post == null) {
            throw new RuntimeException("삭제 대상 게시글 없음");
        }

        // 레코드 삭제
        int cnt = postMapper.destroy(id);
        if(cnt != 1) {
            throw new RuntimeException("게시글 삭제 이상 발생");
        }

        // 파일 삭제 (실패하더라도 처리 속행)
        localFileManager.destroyFile(post.getImage());
    }
}
