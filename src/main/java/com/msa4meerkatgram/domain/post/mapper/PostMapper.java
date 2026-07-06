package com.msa4meerkatgram.domain.post.mapper;

import com.msa4meerkatgram.domain.post.entities.PostMybatis;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PostMapper {
    List<PostMybatis> getPagination(int limit, int offset);
    long getTotal();
    PostMybatis findByPk(long id);
    long countPostsByUserId(long userId);
}
