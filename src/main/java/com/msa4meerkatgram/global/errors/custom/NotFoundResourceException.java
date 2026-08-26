package com.msa4meerkatgram.global.errors.custom;

import com.msa4meerkatgram.global.responses.CustomResponseCode;

public class NotFoundResourceException extends BusinessException {
    public NotFoundResourceException(String message) {
        super(CustomResponseCode.NOT_FOUND_RESOURCE_ERROR, message);
    }
}
