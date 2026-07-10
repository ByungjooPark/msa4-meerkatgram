package com.msa4meerkatgram.global.errors;

import com.msa4meerkatgram.global.responses.constant.CustomResponseCode;
import com.msa4meerkatgram.global.errors.custom.*;
import com.msa4meerkatgram.global.responses.GlobalRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<GlobalRes<Void>> generateErrorResponse(CustomResponseCode customResponseCode) {
        return ResponseEntity.status(customResponseCode.getHttpStatus())
            .body(GlobalRes.<Void>from(customResponseCode));
    }

    @ExceptionHandler(NotRegisteredException.class)
    public ResponseEntity<GlobalRes<Void>> notRegisteredHandle(NotRegisteredException e) {
        log.debug(CustomResponseCode.NOT_REGISTERED_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.NOT_REGISTERED_ERROR);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GlobalRes<Void>> authenticationHandle(AuthenticationException e) {
        log.debug(CustomResponseCode.UNAUTHENTICATED_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.UNAUTHENTICATED_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GlobalRes<Void>> accessDeniedHandle(AccessDeniedException e) {
        log.debug(CustomResponseCode.UNAUTHORIZED_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.UNAUTHORIZED_ERROR);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<GlobalRes<Void>> invalidTokenHandle(InvalidTokenException e) {
        log.debug(CustomResponseCode.INVALID_TOKEN_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.INVALID_TOKEN_ERROR);
    }

    @ExceptionHandler(DeletedRecordException.class)
    public ResponseEntity<GlobalRes<Void>> deletedRecordHandle(DeletedRecordException e) {
        log.debug(CustomResponseCode.NOT_FOUND_DATA_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.NOT_FOUND_DATA_ERROR);
    }

    @ExceptionHandler(DuplicatedRecordException.class)
    public ResponseEntity<GlobalRes<Void>> duplicatedRecordHandle(DuplicatedRecordException e) {
        log.debug(CustomResponseCode.DUPLICATED_DATA_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.DUPLICATED_DATA_ERROR);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GlobalRes<Void>> methodArgumentTypeMismatchHandle(MethodArgumentTypeMismatchException e) {
        log.debug(CustomResponseCode.INVALID_PARAMETER_ERROR.name(), String.format("%s : 필드를 확인해 주세요.", e.getName()));
        return this.generateErrorResponse(CustomResponseCode.INVALID_PARAMETER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalRes<Void>> methodArgumentNotValidHandle(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField, // 필드명
                fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "유효하지 않은 값입니다.",
                (existing, replacement) -> existing // 중복 필드가 있을 경우 기존 값 유지
            ));

        log.debug(CustomResponseCode.INVALID_PARAMETER_ERROR.name(), errors);
        return this.generateErrorResponse(CustomResponseCode.INVALID_PARAMETER_ERROR);
    }

    @ExceptionHandler(FileManagedException.class)
    public ResponseEntity<GlobalRes<Void>> fileManagedHandle(FileManagedException e) {
        log.debug(CustomResponseCode.FILE_MANAGED_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.FILE_MANAGED_ERROR);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<GlobalRes<Void>> sqlHandle(SQLException e) {
        log.error("DB 에러", e);
        return this.generateErrorResponse(CustomResponseCode.DB_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalRes<Void>> othersHandle(Exception e) {
        log.error("시스템 에러", e);
        return this.generateErrorResponse(CustomResponseCode.SYSTEM_ERROR);
    }
}
