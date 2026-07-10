package com.msa4meerkatgram.domain.file.controllers;

import com.msa4meerkatgram.domain.file.responses.FileRes;
import com.msa4meerkatgram.domain.file.services.FileService;
import com.msa4meerkatgram.global.config.openapi.CustomApiResponse;
import com.msa4meerkatgram.global.responses.GlobalRes;
import com.msa4meerkatgram.global.responses.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "파일 API", description = "파일 업로드 관련")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class FileController {
    private final FileService fileService;

    @Operation(summary = "프로필 업로드 처리")
    @CustomApiResponse(value = {
        CustomResponseCode.FILE_MANAGED_ERROR
        ,CustomResponseCode.SYSTEM_ERROR
    })
    @PostMapping("/files/profiles")
    public ResponseEntity<GlobalRes<FileRes>> storeProfile(
        @ModelAttribute MultipartFile file
    ) {
        return ResponseEntity.ok(GlobalRes.success(fileService.storeProfile(file)));
    }

    @Operation(summary = "게시글 이미지 업로드 처리")
    @CustomApiResponse(value = {
        CustomResponseCode.FILE_MANAGED_ERROR
        ,CustomResponseCode.SYSTEM_ERROR
    })
    @PostMapping("/files/posts")
    public ResponseEntity<GlobalRes<FileRes>> storePosts(
        @ModelAttribute MultipartFile file
    ) {
        return ResponseEntity.ok(GlobalRes.success(fileService.storePosts(file)));
    }
}
