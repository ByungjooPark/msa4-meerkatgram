package com.msa4meerkatgram.global.errors.custom;

import com.msa4meerkatgram.global.responses.CustomResponseCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final CustomResponseCode customResponseCode;

    public BusinessException(CustomResponseCode customResponseCode, String message) {
        super(message);
        this.customResponseCode = customResponseCode;
    }
}
