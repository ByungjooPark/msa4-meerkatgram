package com.msa4meerkatgram.global.security.constant;

import lombok.Getter;

@Getter
public enum RolePolicy {
    NORMAL("NORMAL")
    ,SUPER("SUPER");

    private final String role;

    RolePolicy(String role) {
        this.role = role;
    }
}
