package com.msa4meerkatgram.global.errors.constant;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CustomErrorCode {
    NOT_REGISTERED_ERROR(HttpStatus.UNAUTHORIZED, "E01")
    , UNAUTHENTICATED_ERROR(HttpStatus.UNAUTHORIZED, "E02")
    , UNAUTHORIZED_ERROR(HttpStatus.FORBIDDEN, "E03")
    , INVALID_TOKEN_ERROR(HttpStatus.UNAUTHORIZED, "E04")
    , NOT_FOUND_DATA_ERROR(HttpStatus.NOT_FOUND, "E10")
    , DUPLICATED_DATA_ERROR(HttpStatus.CONFLICT, "E11")
    , INVALID_PARAMETER_ERROR(HttpStatus.BAD_REQUEST, "E21")
    , FILE_MANAGED_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E40")
    , DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E80")
    , SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E99")
    ;

    private final HttpStatus httpStatus;
    private final String code;

    CustomErrorCode(HttpStatus httpStatus, String code) {
        this.httpStatus = httpStatus;
        this.code = code;
    }
}
