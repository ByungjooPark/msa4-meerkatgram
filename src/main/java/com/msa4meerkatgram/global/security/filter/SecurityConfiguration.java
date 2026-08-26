package com.msa4meerkatgram.global.security.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 메소드 레벨 권한 제어(@PreAuthorize) 활성화
public class SecurityConfiguration {
    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http
        ,TokenAuthenticationFilter tokenAuthenticationFilter
        ,CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        return http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 비활성 설정
            .httpBasic(AbstractHttpConfigurer::disable) // 화면 생성 비활성 설정
            .formLogin(AbstractHttpConfigurer::disable) // 폼로그인 기능 비활성 설정
            .csrf(AbstractHttpConfigurer::disable) // CSRF 토큰 인증 비활성 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource)) // CORS 설정 추가
            .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // 필터 등록
            .authorizeHttpRequests(req -> req.anyRequest().permitAll()) // 인증 여부와 무관하게 모든 요청 통과 - 인가는 각 Controller의 @PreAuthorize가 담당
            .build();
    }


}
