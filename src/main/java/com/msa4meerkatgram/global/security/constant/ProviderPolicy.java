package com.msa4meerkatgram.global.security.constant;

import lombok.Getter;

@Getter
public enum ProviderPolicy {
    NONE("NONE")
    ,KAKAO("KAKAO")
    ,GOOGLE("GOOGLE");

    private final String provider;

    ProviderPolicy(String provider) {
        this.provider = provider;
    }
}
