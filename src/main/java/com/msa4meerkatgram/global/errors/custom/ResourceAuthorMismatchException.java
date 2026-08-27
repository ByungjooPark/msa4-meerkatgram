package com.msa4meerkatgram.global.errors.custom;

import com.msa4meerkatgram.global.responses.CustomResponseCode;

public class ResourceAuthorMismatchException extends BusinessException {
    public ResourceAuthorMismatchException(String message) {
        super(CustomResponseCode.RESOURCE_AUTHOR_MISMATCH_ERROR, message);
    }
}
