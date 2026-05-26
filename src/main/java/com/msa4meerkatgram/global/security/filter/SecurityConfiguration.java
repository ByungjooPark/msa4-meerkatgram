package com.msa4meerkatgram.global.security.filter;

import com.msa4meerkatgram.global.config.CorsConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final CorsConfig corsConfig;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 프론트엔드 도메인 설정 (정확한 Origin 지정)
        configuration.setAllowedOrigins(corsConfig.allowedOrigins());

        // 허용할 HTTP Method 지정
        configuration.setAllowedMethods(List.of(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name() // Preflight 요청 허용 필수
        ));

        // 허용할 헤더 지정 (jjwt 사용 시 Authorization 헤더 필수)
        configuration.setAllowedHeaders(List.of(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.ACCEPT
        ));

        // 자격증명(Cookie, 인증 헤더 등) 포함 여부 결정
        // true 설정 시 allowedOrigins에 "*"를 사용할 수 없으나, 이미 특정 도메인을 지정했으므로 안전합니다.
        configuration.setAllowCredentials(true);

        // 브라우저가 Preflight 요청 결과를 캐싱할 시간 (초 단위)
        configuration.setMaxAge(corsConfig.maxAge());

        // 모든 API 경로에 위 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SecurityExceptionHandler securityExceptionHandler, TokenAuthenticationFilter tokenAuthenticationFilter) throws Exception {
        return http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 비활성 설정
            .httpBasic(AbstractHttpConfigurer::disable) // SSR이 아니므로 화면생성 비활성 설정
            .formLogin(AbstractHttpConfigurer::disable) // SSR이 아니므로 폼로그인 기능 비활성화 설정
            .csrf(AbstractHttpConfigurer::disable) // SSR이 아니므로 CSRF 토큰 인증 비활성화 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 추가
            .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(req -> // 리퀘스트에 대한 권한 설정
                req.requestMatchers(HttpMethod.GET, SecurityUrlRegistry.AUTH_REQUIRED_GET_URLS).authenticated()
                    .requestMatchers(HttpMethod.POST, SecurityUrlRegistry.AUTH_REQUIRED_POST_URLS).authenticated()
                    .requestMatchers(HttpMethod.PATCH, SecurityUrlRegistry.AUTH_REQUIRED_PATCH_URLS).authenticated()
                    .requestMatchers(HttpMethod.DELETE, SecurityUrlRegistry.AUTH_REQUIRED_DELETE_URLS).authenticated()
                    .anyRequest().permitAll() // 그 외는 권한 불필요
            )
            .exceptionHandling(e -> e
                .authenticationEntryPoint(securityExceptionHandler)
                .accessDeniedHandler(securityExceptionHandler)
            )
            .build();
    }
}
