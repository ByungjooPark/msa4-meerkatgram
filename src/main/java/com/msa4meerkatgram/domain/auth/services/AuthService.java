package com.msa4meerkatgram.domain.auth.services;

import com.msa4meerkatgram.domain.auth.mapper.AuthMapper;
import com.msa4meerkatgram.domain.auth.requests.LoginRequestDTO;
import com.msa4meerkatgram.domain.auth.requests.RegistrationRequestDTO;
import com.msa4meerkatgram.domain.auth.responses.AuthResponseDTO;
import com.msa4meerkatgram.domain.post.mapper.PostMapper;
import com.msa4meerkatgram.domain.user.entities.User;
import com.msa4meerkatgram.domain.user.mapper.UserMapper;
import com.msa4meerkatgram.domain.user.responses.UserResponseDTO;
import com.msa4meerkatgram.global.errors.custom.DuplicatedResourceException;
import com.msa4meerkatgram.global.errors.custom.InvalidTokenException;
import com.msa4meerkatgram.global.errors.custom.NotRegisteredException;
import com.msa4meerkatgram.global.security.constant.ProviderPolicy;
import com.msa4meerkatgram.global.security.constant.RolePolicy;
import com.msa4meerkatgram.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final JwtProvider jwtProvider;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final PostMapper postMapper;

    @Transactional(rollbackFor = Exception.class)
    public AuthResponseDTO login(LoginRequestDTO loginRequestDTO) {
        // 유저정보 획득
        User user = userMapper.findByEmail(loginRequestDTO.email());

        // 유저 가입 여부 확인
        if(user == null) {
            throw new NotRegisteredException("아이디와 비밀번호를 확인해주세요.");
        }

        // 비밀번호 체크
        if(!passwordEncoder.matches(loginRequestDTO.password(), user.getPassword())) {
            throw new NotRegisteredException("아이디와 비밀번호를 확인해주세요.");
        }

        return this.generateAuthentication(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthResponseDTO reissue(String refreshToken) {
        long id = Long.parseLong(jwtProvider.extractClaims(refreshToken).getSubject());

        // 유저 획득
        User user = userMapper.findByPk(id);

        // 유저 가입 여부 확인 및 비로그인 상태 확인
        if(user == null || user.getRefreshToken() == null) {
            throw new InvalidTokenException("유효하지 않은 회원의 토큰입니다.");
        }

        // 리프래시 토큰 비교
        if(!user.getRefreshToken().equals(refreshToken)) {
            throw new InvalidTokenException("토큰이 일치하지 않습니다.");
        }

        return this.generateAuthentication(user);
    }


    /**
     * 액세스토큰 및 리프래시토큰 생성 후, 리프래시 토큰 DB 저장, AuthRes로 반환
     * @param user 유저 Entity
     * @return AuthResponseDTO
     */
    private AuthResponseDTO generateAuthentication(User user) {
        // 작성 게시글 수 획득
        long countPosts = postMapper.countPostsByUserId(user.getId());

        // 토큰 생성
        String newAccessToken = jwtProvider.generateAccessToken(user);
        String newRefreshToken = jwtProvider.generateRefreshToken(user);

        // 리프래시 토큰을 DB 저장
        authMapper.updateRefreshToken(user.getId(), newRefreshToken);

        // 리턴 (리프래시 토큰의 쿠키 저장은 Controller의 책임)
        return AuthResponseDTO.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .user(
                UserResponseDTO.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .nick(user.getNick())
                    .role(user.getRole())
                    .profile(user.getProfile())
                    .createdAt(user.getCreatedAt())
                    .countPosts(countPosts)
                    .build()
            )
            .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void logout(long id) {
        // 유저 정보 획득
        User user = userMapper.findByPk(id);

        if(user == null) {
            throw new InvalidTokenException("유효하지 않은 회원의 토큰입니다.");
        }

        // DB에 저장한 리프래시 토큰 파기 (쿠키 파기는 Controller의 책임)
        authMapper.updateRefreshToken(id, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void registration(RegistrationRequestDTO registrationRequestDTO) {
        // 유저 정보 획득
        User user = userMapper.findByEmail(registrationRequestDTO.email());

        if(user != null) {
            throw new DuplicatedResourceException("이미 가입된 회원입니다.");
        }

        User newUser = new User();
        newUser.setEmail(registrationRequestDTO.email());
        newUser.setPassword(passwordEncoder.encode(registrationRequestDTO.password()));
        newUser.setNick(registrationRequestDTO.nick());
        newUser.setProfile(registrationRequestDTO.profile());
        newUser.setProvider(ProviderPolicy.NONE.getProvider());
        newUser.setRole(RolePolicy.NORMAL.getRole());
        authMapper.create(newUser);
    }
}
