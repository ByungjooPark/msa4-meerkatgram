package com.msa4meerkatgram.domain.auth.mapper;

import com.msa4meerkatgram.domain.user.entities.UserMybatis;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthMapper {
    int updateRefreshToken(long id, String refreshToken);
    int create(UserMybatis user);
}
