package com.msa4meerkatgram.global.errors.custom;

public class FileManagedException extends RuntimeException {
    public FileManagedException(String message) {
        super(message);
    }
}
