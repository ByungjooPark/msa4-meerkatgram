package com.msa4meerkatgram.global.errors;

import com.msa4meerkatgram.global.errors.custom.FileStorageException;
import com.msa4meerkatgram.global.errors.custom.InvalidTokenException;
import com.msa4meerkatgram.global.errors.custom.NotRegisteredException;
import com.msa4meerkatgram.global.responses.GlobalRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotRegisteredException.class)
    public ResponseEntity<GlobalRes<String>> authenticationHandle(NotRegisteredException e) {
        String message = "로그인 에러";

        return ResponseEntity.status(401).body(
            GlobalRes.<String>builder()
                .code("E01")
                .message(message)
                .data(e.getMessage())
                .build()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GlobalRes<String>> authenticationHandle(AuthenticationException e) {
        String message = "인증이 필요합니다.";

        return ResponseEntity.status(401).body(
            GlobalRes.<String>builder()
                .code("E02")
                .message(message)
                .build()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GlobalRes<String>> accessDeniedHandle(AccessDeniedException e) {
        String message = "접근 권한이 없습니다.";

        return ResponseEntity.status(403).body(
            GlobalRes.<String>builder()
                .code("E03")
                .message(message)
                .build()
        );
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<GlobalRes<String>> InvalidTokenHandle(InvalidTokenException e) {
        String message = "토큰 이상";

        return ResponseEntity.status(400).body(
            GlobalRes.<String>builder()
                .code("E04")
                .message(message)
                .data(e.getMessage())
                .build()
        );
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<GlobalRes<String>> notFoundHandle(Exception e) {
        return ResponseEntity.status(404).body(
            GlobalRes.<String>builder()
                .code("E20")
                .message("Not Found Error")
                .data("찾을 수 없는 리소스입니다.")
                .build()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GlobalRes<String>> methodArgumentTypeMismatchHandle(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(400).body(
                GlobalRes.<String>builder()
                        .code("E21")
                        .message("요청 파라미터에 이상이 있습니다.")
                        .data(String.format("%s : 필드를 확인해 주세요.", e.getName()))
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalRes<List<String>>> methodArgumentNotValidHandle(MethodArgumentNotValidException e) {
        return ResponseEntity.status(400).body(
                GlobalRes.<List<String>>builder()
                        .code("E21")
                        .message("요청 파라미터에 이상이 있습니다.")
                        .data(
                                e.getBindingResult()
                                        .getAllErrors()
                                        .stream()
                                        .map(item -> String.format("%s : 잘못된 값입니다.", item.getObjectName()))
                                        .toList()
                        )
                        .build()
        );
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<GlobalRes<String>> fileStorageHandle(FileStorageException e) {
        log.error("파일 생성 에러: {}\n{}", e.getMessage(), Arrays.toString(e.getStackTrace()));

        return ResponseEntity.status(400).body(
                GlobalRes.<String>builder()
                        .code("E30")
                        .message("파일 생성 에러")
                        .data(e.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<GlobalRes<String>> fileStorageHandle(RuntimeException e) {
        return ResponseEntity.status(400).body(
            GlobalRes.<String>builder()
                .code("E30")
                .message("런타임 에러")
                .data(e.getMessage())
                .build()
        );
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<GlobalRes<String>> sqlExceptionHandle(SQLException e) {
        log.error(e.getMessage());

        return ResponseEntity.status(500).body(
            GlobalRes.<String>builder()
                .code("E80")
                .message("DB 에러 발생")
                .data("현재 서비스 이용 불가합니다.\n잠시후 다시 시도해 주십시오.")
                .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalRes<String>> othersHandle(Exception e) {
        log.error("시스템 에러: {}\n{}", e.getMessage(), Arrays.toString(e.getStackTrace()));
        return ResponseEntity.status(500).body(
                GlobalRes.<String>builder()
                        .code("E99")
                        .message("시스템 에러")
                        .data("현재 서비스 이용이 불가합니다. 잠시후 다시 시도해 주십시오.")
                        .build()
        );
    }
}
