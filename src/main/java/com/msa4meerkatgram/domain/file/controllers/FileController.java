package com.msa4meerkatgram.domain.file.controllers;

import com.msa4meerkatgram.domain.file.responses.FileRes;
import com.msa4meerkatgram.domain.file.services.FileService;
import com.msa4meerkatgram.global.responses.GlobalRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class FileController {
    private final FileService fileService;

    @PostMapping("/files/profiles")
    public ResponseEntity<GlobalRes<FileRes>> storeProfile(
        @ModelAttribute MultipartFile file
    ) {
        return ResponseEntity.ok(GlobalRes.success(fileService.storeProfile(file)));
    }

    @PostMapping("/files/posts")
    public ResponseEntity<GlobalRes<FileRes>> storePosts(
        @ModelAttribute MultipartFile file
    ) {
        return ResponseEntity.ok(GlobalRes.success(fileService.storePosts(file)));
    }
}
