package com.msa4meerkatgram.global.errors.custom;

import com.msa4meerkatgram.global.responses.CustomResponseCode;

public class DuplicatedResourceException extends BusinessException {
    public DuplicatedResourceException(String message) {
        super(CustomResponseCode.DUPLICATED_RESOURCE_ERROR, message);
    }
}
