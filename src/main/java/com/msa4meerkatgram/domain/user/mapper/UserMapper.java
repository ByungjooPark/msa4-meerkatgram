package com.msa4meerkatgram.domain.user.mapper;

import com.msa4meerkatgram.domain.user.entities.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    User findByPk(long id);
    User findByEmail(String email);
}
