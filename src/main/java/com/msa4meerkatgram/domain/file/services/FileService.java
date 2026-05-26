package com.msa4meerkatgram.domain.file.services;

import com.msa4meerkatgram.domain.file.responses.FileRes;
import com.msa4meerkatgram.global.util.file.FileConfig;
import com.msa4meerkatgram.global.util.file.LocalFileManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileService {
    private final LocalFileManager localFileManager;
    private final FileConfig fileConfig;

    public FileRes storePostImage(MultipartFile file) {
        // 파일 경로 생성
        String path = localFileManager.generatePostPath(file);

        // 파일 저장
        localFileManager.saveFile(file, path);

        return FileRes.builder().fileUrl(fileConfig.serverUri() + path).build();
    }

    public FileRes storeProfile(MultipartFile file) {
        // 파일 경로 생성
        String path = localFileManager.generateProfilePath(file);

        // 파일 저장
        localFileManager.saveFile(file, path);

        return FileRes.builder().fileUrl(fileConfig.serverUri() + path).build();
    }
}
