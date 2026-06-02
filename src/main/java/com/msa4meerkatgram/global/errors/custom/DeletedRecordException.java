package com.msa4meerkatgram.global.errors.custom;

public class DeletedRecordException extends RuntimeException {
    public DeletedRecordException(String message) {
        super(message);
    }
}
