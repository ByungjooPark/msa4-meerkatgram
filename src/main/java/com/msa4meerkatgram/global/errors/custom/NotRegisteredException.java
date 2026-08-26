package com.msa4meerkatgram.global.errors.custom;

import com.msa4meerkatgram.global.responses.CustomResponseCode;

public class NotRegisteredException extends BusinessException {
    public NotRegisteredException(String message) {
        super(CustomResponseCode.NOT_REGISTERED_ERROR, message);
    }
}
