package com.msa4meerkatgram.global.security.filter;

public final class SecurityUrlRegistry {
    private SecurityUrlRegistry() {} // 인스턴스화 방지

    // -------------------------
    // 모두 허용할 화이트리스트
    // -------------------------
    public static final String[] PUBLIC_GET_URLS = {
        "/api/auth/social/{provider}"
        ,"/api/auth/social/callback/{provider}"
        ,"/api/posts"
    };
    public static final String[] PUBLIC_POST_URLS = {
        "/api/auth/login"
        ,"/api/auth/reissue"
        ,"/api/users"
    };

    // -------------------------
    // 인증이 반드시 필요한 블랙리스트
    // -------------------------
    public static final String[] AUTH_REQUIRED_GET_URLS = {
        "/api/posts/{id}"
    };
    public static final String[] AUTH_REQUIRED_POST_URLS = {
        "/api/logout"
        ,"/api/posts"
    };
    public static final String[] AUTH_REQUIRED_PATCH_URLS = {
        "/api/users"
    };
    public static final String[] AUTH_REQUIRED_DELETE_URLS = {
        "/api/posts"
    };
}
