package com.msa4meerkatgram.global.annotations.openapi;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
    responseCode = "400"
    , description = "유효성 검사 실패"
    , content = @Content(
        mediaType = "application/json"
        , examples = {
            @ExampleObject(
                name = "유효성 검사 실패 에러"
                , value = """
                            {
                                "code": "E21"
                                ,"message": "Bad Request"
                            }
                        """
            )
        }
    )
)
public @interface ApiNotValidErrorResponse {
}
