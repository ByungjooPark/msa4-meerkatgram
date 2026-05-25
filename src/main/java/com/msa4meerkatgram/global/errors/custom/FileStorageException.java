package com.msa4meerkatgram.global.errors.custom;

public class FileStorageException extends RuntimeException {
    // 메시지만 받는 생성자 (단순 에러 로그용)
    public FileStorageException(String message) {
        super(message);
    }

    // 메시지와 원인 예외(Throwable cause)를 모두 받는 생성자
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
