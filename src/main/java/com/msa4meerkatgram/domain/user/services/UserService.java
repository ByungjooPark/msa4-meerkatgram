package com.msa4meerkatgram.domain.user.services;

import com.msa4meerkatgram.domain.user.constant.ProviderPolicy;
import com.msa4meerkatgram.domain.user.constant.RolePolicy;
import com.msa4meerkatgram.domain.user.entities.User;
import com.msa4meerkatgram.domain.user.mapper.UserMapper;
import com.msa4meerkatgram.domain.user.requests.RegistrationReq;
import com.msa4meerkatgram.domain.user.responses.UserRes;
import com.msa4meerkatgram.global.util.file.FileConfig;
import com.msa4meerkatgram.global.util.file.LocalFileManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final FileConfig fileConfig;
    private final LocalFileManager localFileManager;
    private final PasswordEncoder passwordEncoder;

    public UserRes show(long id) {
        User user = userMapper.findByPk(id);

        return UserRes.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nick(user.getNick())
                .role(user.getRole())
                .profile(user.getProfile())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public UserRes create(RegistrationReq RegistrationReq) {
        // 유저 획득
        User chkUser = userMapper.findByEmail(RegistrationReq.email());

        // 유저 가입 여부 확인
        if(chkUser != null) {
            throw new RuntimeException("이미 가입된 회원입니다.");
        }

        // 파일 경로 생성
        String profilePath = localFileManager.generateProfilePath(RegistrationReq.profile());
        String fullPath = fileConfig.serverUri() + profilePath;

        // DB에 유저정보 저장
        User user = new User();
        user.setEmail(RegistrationReq.email());
        user.setPassword(passwordEncoder.encode(RegistrationReq.password()));
        user.setNick(RegistrationReq.nick());
        user.setProfile(fullPath);
        user.setProvider(ProviderPolicy.NONE.getProvider());
        user.setRole(RolePolicy.NORMAL.getRole());
        userMapper.create(user);

        // 프로필 저장
        localFileManager.saveFile(RegistrationReq.profile(), profilePath);

        return UserRes.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nick(user.getNick())
                .role(user.getRole())
                .profile(user.getProfile())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
