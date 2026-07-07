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

    // @Transactional(rollbackFor = Exception.class)
    // public PostMybatis store(long userId, PostStoreReq postStoreReq) {
    //     // 작성 게시글 객체 생성
    //     PostMybatis post = PostMybatis.builder()
    //         .userId(userId)
    //         .content(postStoreReq.content())
    //         .image(postStoreReq.image())
    //         .build();
    //
    //     // 게시글 작성 처리
    //     postMapper.store(post);
    //
    //     // 새로 작성한 게시글 획득 및 반환
    //     return postMapper.findByPk(post.getId());
    // }
}
