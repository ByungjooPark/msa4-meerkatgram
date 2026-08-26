package com.msa4meerkatgram.global.errors.custom;

import com.msa4meerkatgram.global.responses.CustomResponseCode;

public class FileManagedException extends BusinessException {
    public FileManagedException(String message) {
        super(CustomResponseCode.FILE_MANAGED_ERROR, message);
    }
}
