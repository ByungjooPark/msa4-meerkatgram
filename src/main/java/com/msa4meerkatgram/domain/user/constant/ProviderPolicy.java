package com.msa4meerkatgram.domain.user.constant;

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
