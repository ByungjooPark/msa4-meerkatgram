package com.msa4meerkatgram.global.errors;

import com.msa4meerkatgram.global.errors.custom.BusinessException;
import com.msa4meerkatgram.global.errors.custom.FileManagedException;
import com.msa4meerkatgram.global.responses.CustomResponseCode;
import com.msa4meerkatgram.global.responses.GlobalRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<GlobalRes<Void>> generateErrorResponse(CustomResponseCode customResponseCode) {
        return ResponseEntity.status(customResponseCode.getHttpStatus()).body(
            GlobalRes.<Void>builder()
                .code(customResponseCode.getCode())
                .message(customResponseCode.name())
                .build()
        );
    }

    /**
     * 미어켓그램의 커스텀 Exceptions 처리
     * @param e BusinessException
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GlobalRes<Void>> handle(BusinessException e) {
        // FileManagedException(파일 저장/삭제 실패)만 운영상 즉시 확인이 필요해 error 레벨, 나머지는 debug 레벨로 남긴다
        if (e instanceof FileManagedException) {
            log.error(e.getMessage(), e);
        } else {
            log.debug(e.getMessage(), e);
        }
        return this.generateErrorResponse(e.getCustomResponseCode());
    }

    // ------------------------------------
    // 이하 Spring에서 발생하는 Exceptions
    // ------------------------------------

    // v1은 아직 Security 설정에서 URL 패턴별로 인증 여부를 판단한다(SecurityConfiguration 참고).
    // @PreAuthorize 전환 전까지는 AuthenticationException(미인증)과 AccessDeniedException(권한부족)이
    // 분리되어 발생하므로 핸들러도 분리해서 둔다 — v2는 @PreAuthorize만 쓰기 때문에 이 핸들러가 없다.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GlobalRes<Void>> handle(AuthenticationException e) {
        log.debug(CustomResponseCode.UNAUTHENTICATED_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.UNAUTHENTICATED_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GlobalRes<Void>> handle(AccessDeniedException e) {
        log.debug(CustomResponseCode.UNAUTHORIZED_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.UNAUTHORIZED_ERROR);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GlobalRes<Void>> handle(MethodArgumentTypeMismatchException e) {
        log.debug("{}\n{}", CustomResponseCode.INVALID_PARAMETER_ERROR.name(), String.format("%s : 필드를 확인해 주세요.", e.getName()));
        return this.generateErrorResponse(CustomResponseCode.INVALID_PARAMETER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalRes<Void>> handle(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField, // 필드명
                fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "유효하지 않은 값입니다.",
                (existing, replacement) -> existing // 중복 필드가 있을 경우 기존 값 유지
            ));

        log.debug("{}\n{}", CustomResponseCode.INVALID_PARAMETER_ERROR.name(), errors);
        return this.generateErrorResponse(CustomResponseCode.INVALID_PARAMETER_ERROR);
    }

    // RequestBody 자체가 없을 경우 에러
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GlobalRes<Void>> handle(HttpMessageNotReadableException e) {
        log.debug(CustomResponseCode.INVALID_PARAMETER_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.INVALID_PARAMETER_ERROR);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GlobalRes<Void>> handle(NoResourceFoundException e) {
        log.debug(CustomResponseCode.NOT_FOUND_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.NOT_FOUND_ERROR);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<GlobalRes<Void>> handle(DuplicateKeyException e) {
        log.error("DB 에러", e);
        return this.generateErrorResponse(CustomResponseCode.DB_DUPLICATED_KEY_ERROR);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<GlobalRes<Void>> handle(DataAccessException e) {
        log.error("DB 에러", e);
        return this.generateErrorResponse(CustomResponseCode.DB_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalRes<Void>> handle(Exception e) {
        log.error("시스템 에러", e);
        return this.generateErrorResponse(CustomResponseCode.SYSTEM_ERROR);
    }
}
