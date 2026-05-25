package com.msa4meerkatgram.global.errors.custom;

public class InvalidTokenException extends RuntimeException {
    // 메시지만 받는 생성자 (단순 에러 로그용)
    public InvalidTokenException(String message) {
        super(message);
    }
}
