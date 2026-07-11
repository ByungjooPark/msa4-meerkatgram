package com.msa4meerkatgram.global.security.filter;

import com.msa4meerkatgram.global.config.CorsConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final CorsConfig corsConfig;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http
        ,TokenAuthenticationFilter tokenAuthenticationFilter
    ) throws Exception {
        return http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 비활성 설정
            .httpBasic(AbstractHttpConfigurer::disable) // 화면 생성 비활성 설정
            .formLogin(AbstractHttpConfigurer::disable) // 폼로그인 기능 비활성 설정
            .csrf(AbstractHttpConfigurer::disable) // CSRF 토큰 인증 비활성 설정
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(corsConfig.allowedOrigins());
                config.setAllowedMethods(List.of(HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(), HttpMethod.PATCH.name(), HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()));
                config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT));
                config.setAllowCredentials(true);
                config.setMaxAge(corsConfig.maxAge());
                return config;
            }))// CORS 설정 추가
            .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // 필터 등록
            .authorizeHttpRequests(req -> req.anyRequest().permitAll()) // 리퀘스트에 대한 권한 설정
            .build();
    }
}
