package com.msa4meerkatgram.global.errors.custom;

public class NotRegisteredException extends RuntimeException {
    public NotRegisteredException(String message) {
        super(message);
    }
}
