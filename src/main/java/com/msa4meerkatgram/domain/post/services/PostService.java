package com.msa4meerkatgram.domain.post.services;

import com.msa4meerkatgram.domain.post.entities.Post;
import com.msa4meerkatgram.domain.post.mapper.PostMapper;
import com.msa4meerkatgram.domain.post.requests.PostIndexRequestDTO;
import com.msa4meerkatgram.domain.post.requests.PostStoreRequestDTO;
import com.msa4meerkatgram.domain.post.responses.PostIndexResponseDTO;
import com.msa4meerkatgram.global.errors.custom.NotFoundResourceException;
import com.msa4meerkatgram.global.errors.custom.ResourceAuthorMismatchException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostMapper postMapper;

    public PostIndexResponseDTO index(PostIndexRequestDTO postIndexRequestDTO) {
        int offset = (postIndexRequestDTO.page() - 1) * postIndexRequestDTO.limit();

        // 특정 페이지 게시글 조회
        List<Post> posts = postMapper.getPagination(postIndexRequestDTO.limit(), offset);

        // 토탈 획득
        long total = postMapper.getTotal();
        boolean isLastPage = offset + postIndexRequestDTO.limit() >= total;

        // 컨트롤러 전달
        return PostIndexResponseDTO.from(total, isLastPage, posts);
    }

    public Post show(long id) {
        Post post = postMapper.findByPk(id);

        if(post == null) {
            throw new NotFoundResourceException("이미 삭제된 게시글입니다.");
        }

        return post;
    }

    @Transactional(rollbackFor = Exception.class)
    public Post store(long userId, PostStoreRequestDTO postStoreRequestDTO) {
        // 작성 게시글 객체 생성
        Post post = Post.builder()
            .userId(userId)
            .content(postStoreRequestDTO.content())
            .image(postStoreRequestDTO.image())
            .build();

        // 게시글 작성 처리
        postMapper.store(post);

        // 새로 작성한 게시글 획득 및 반환
        return postMapper.findByPk(post.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void destroy(long id, long userId) {
        // 게시글 조회
        Post post = postMapper.findByPk(id);

        if(post == null) {
            throw new NotFoundResourceException("이미 삭제된 게시글: " + id);
        }

        // 작성자 체크
        if(post.getUserId() != userId) {
            throw new ResourceAuthorMismatchException("게시글 삭제 실패: 작성자 다름");
        }

        postMapper.destroy(id);
    }
}
