package com.msa4meerkatgram.domain.file.controleers;

import com.msa4meerkatgram.domain.file.responses.FileRes;
import com.msa4meerkatgram.domain.file.services.FileService;
import com.msa4meerkatgram.global.responses.GlobalRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class FileController {
    private final FileService fileService;

    @PostMapping("/images/posts")
    public ResponseEntity<GlobalRes<FileRes>> storePostImage(
        @ModelAttribute MultipartFile file
    ) {
        return ResponseEntity.status(200).body(
            GlobalRes.<FileRes>builder()
                .code("00")
                .message("정상 처리")
                .data(fileService.storePostImage(file))
                .build()
        );
    }

    @PostMapping("/images/profiles")
    public ResponseEntity<GlobalRes<FileRes>> storeProfile(
        @ModelAttribute MultipartFile file
    ) {
        return ResponseEntity.status(200).body(
            GlobalRes.<FileRes>builder()
                .code("00")
                .message("정상 처리")
                .data(fileService.storeProfile(file))
                .build()
        );
    }
}
