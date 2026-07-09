package com.msa4meerkatgram.global.responses;

public record GlobalErrorRes (
    String code
    , String message
) {
    public static  GlobalErrorRes from(String code, String message) {
        return new GlobalErrorRes(code, message);
    }
}
