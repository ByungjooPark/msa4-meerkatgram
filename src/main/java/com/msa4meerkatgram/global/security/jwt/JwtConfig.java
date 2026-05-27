package com.msa4meerkatgram.global.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtConfig(
    boolean secure,
    String issuer,
    String type,
    int accessTokenExpiry,
    int refreshTokenExpiry,
    String refreshTokenCookieName,
    int refreshTokenCookieExpiry,
    String secret,
    String headerKey,
    String scheme,
    String reissUri
) {
}
