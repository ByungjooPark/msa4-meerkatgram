package com.msa4meerkatgram.global.errors.custom;

import com.msa4meerkatgram.global.responses.CustomResponseCode;

public class InvalidTokenException extends BusinessException {
    public InvalidTokenException(String message) {
        super(CustomResponseCode.INVALID_TOKEN_ERROR, message);
    }
}
