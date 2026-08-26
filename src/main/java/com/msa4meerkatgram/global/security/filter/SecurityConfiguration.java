package com.msa4meerkatgram.global.security.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http
        ,SecurityExceptionHandler securityExceptionHandler
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
            .authorizeHttpRequests(req ->
                // 리퀘스트에 대한 권한 설정
                req.requestMatchers(HttpMethod.GET, SecurityUrlRegistry.AUTH_REQUIRED_GET_URLS).authenticated()
                    .requestMatchers(HttpMethod.POST, SecurityUrlRegistry.AUTH_REQUIRED_POST_URLS).authenticated()
                    .requestMatchers(HttpMethod.PUT, SecurityUrlRegistry.AUTH_REQUIRED_PUT_URLS).authenticated()
                    .requestMatchers(HttpMethod.PATCH, SecurityUrlRegistry.AUTH_REQUIRED_PATCH_URLS).authenticated()
                    .requestMatchers(HttpMethod.DELETE, SecurityUrlRegistry.AUTH_REQUIRED_DELETE_URLS).authenticated()
                    .anyRequest().permitAll() // 그 외는 인증 불필요
            )
            .exceptionHandling(e ->
                e.authenticationEntryPoint(securityExceptionHandler)
                    .accessDeniedHandler(securityExceptionHandler)
            )
            .build();
    }


}
