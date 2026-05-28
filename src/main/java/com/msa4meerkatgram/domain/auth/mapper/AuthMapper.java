package com.msa4meerkatgram.domain.auth.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthMapper {
    int updateRefreshToken(long id, String refreshToken);
}
