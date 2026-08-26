package com.msa4meerkatgram.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CorsBeanConfig {
    private final CorsConfig corsConfig;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 프론트앤드 도메인 설정
        configuration.setAllowedOrigins(corsConfig.allowedOrigins());

        // 허용할 HTTP Method 지정
        configuration.setAllowedMethods(List.of(
            HttpMethod.GET.name()
            ,HttpMethod.POST.name()
            ,HttpMethod.PUT.name()
            ,HttpMethod.PATCH.name()
            ,HttpMethod.DELETE.name()
            ,HttpMethod.OPTIONS.name() // preflight 요청 허용
        ));

        // 허용할 헤더 지정
        configuration.setAllowedHeaders(List.of(
            HttpHeaders.AUTHORIZATION
            ,HttpHeaders.CONTENT_TYPE
            ,HttpHeaders.ACCEPT
        ));

        // 자격증명(Cookie, 인증 헤더 정보 등등) 포함 여부 설정
        configuration.setAllowCredentials(true);

        // 브라우저가 preflight 요청 결과를 캐시할 시간(초 단위) 설정
        configuration.setMaxAge(corsConfig.maxAge());

        // 모든 API경로에 위 설정을 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
