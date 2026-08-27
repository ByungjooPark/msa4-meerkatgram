package com.msa4meerkatgram.global.errors;

import com.msa4meerkatgram.global.errors.custom.BusinessException;
import com.msa4meerkatgram.global.errors.custom.FileManagedException;
import com.msa4meerkatgram.global.responses.CustomResponseCode;
import com.msa4meerkatgram.global.responses.GlobalResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private ResponseEntity<GlobalResponseDTO<Void>> generateErrorResponse(CustomResponseCode customResponseCode) {
        return ResponseEntity.status(customResponseCode.getHttpStatus()).body(GlobalResponseDTO.from(customResponseCode));
    }

    /**
     * 미어켓그램의 커스텀 Exceptions 처리
     * @param e BusinessException
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handle(BusinessException e) {
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

    // SecurityConfiguration이 이제 URL 레벨에서는 전부 permitAll이라, 인증/인가 실패는
    // @PreAuthorize에서만 발생한다 - 이유(미인증 vs 권한부족)와 무관하게 항상 AccessDeniedException으로
    // 온다. 익명 사용자인지 SecurityContext로 직접 구분해서 401/403을 나눠준다.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handle(AccessDeniedException e) {
        log.debug(CustomResponseCode.UNAUTHORIZED_ERROR.name(), e);

        // 현재 로그인한 사용자의 정보를 컨텍스트에서 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 로그인하지 않은 익명 사용자가 접근한 경우 (인증 실패 - 401)
        if (authentication instanceof AnonymousAuthenticationToken) {
            return this.generateErrorResponse(CustomResponseCode.UNAUTHENTICATED_ERROR);
        }

        // 로그인은 했으나 권한(Role)이 부족한 경우 (인가 실패 - 403)
        return this.generateErrorResponse(CustomResponseCode.UNAUTHORIZED_ERROR);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handle(MethodArgumentTypeMismatchException e) {
        log.debug("{}\n{}", CustomResponseCode.INVALID_PARAMETER_ERROR.name(), String.format("%s : 필드를 확인해 주세요.", e.getName()));
        return this.generateErrorResponse(CustomResponseCode.INVALID_PARAMETER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handle(MethodArgumentNotValidException e) {
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
    public ResponseEntity<GlobalResponseDTO<Void>> handle(HttpMessageNotReadableException e) {
        log.debug(CustomResponseCode.INVALID_PARAMETER_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.INVALID_PARAMETER_ERROR);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handle(NoResourceFoundException e) {
        log.debug(CustomResponseCode.NOT_FOUND_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.NOT_FOUND_ERROR);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handle(DuplicateKeyException e) {
        log.error("DB 에러", e);
        return this.generateErrorResponse(CustomResponseCode.DB_DUPLICATED_KEY_ERROR);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handle(DataAccessException e) {
        log.error("DB 에러", e);
        return this.generateErrorResponse(CustomResponseCode.DB_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handle(Exception e) {
        log.error("시스템 에러", e);
        return this.generateErrorResponse(CustomResponseCode.SYSTEM_ERROR);
    }
}
