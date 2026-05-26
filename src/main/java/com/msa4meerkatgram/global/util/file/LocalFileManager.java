package com.msa4meerkatgram.global.util.file;

import com.msa4meerkatgram.global.errors.custom.FileStorageException;
import io.jsonwebtoken.lang.Strings;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
public class LocalFileManager {
    private final List<String> ALLOW_EXTENTION_LIST =  List.of("image/jpg", "image/jpeg", "image/png", "image/webp");
    private final FileConfig fileConfig;

    public LocalFileManager(FileConfig fileConfig) {
        this.fileConfig = fileConfig;
    }

    // 확장자 추출을 Optional로 변경하여 안전성 확보
    public String getExtension(MultipartFile file) {
        // 파일 존재 체크
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("파일 저장 실패: 파일 확장자 획득 실패(파일 없음)");
        }

        // 파일 확장자 검증
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            throw new FileStorageException("파일 저장 실패: 파일 확장자 획득 실패(파일명 이상)");
        }
        String extractExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        if(!ALLOW_EXTENTION_LIST.contains("image/" + extractExtension)) {
            throw new FileStorageException(String.format("파일 저장 실패: 허용하지 않는 확장자(.%s)", extractExtension));
        }

        return extractExtension;
    }

    // 랜덤 파일명 생성 (확장자 미지정 시 기본적으로 .png 대입 혹은 에러 처리 유도)
    private String generateFileName(MultipartFile file) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate now = LocalDate.now();

        return now.format(dateFormatter) + "_" + UUID.randomUUID() + "." + this.getExtension(file);
    }

    // URL의 path로 사용될 경로 생성
    public String generateProfilePath(MultipartFile file) {
        return fileConfig.profilePath() + "/" + this.generateFileName(file);
    }

    public String generatePostPath(MultipartFile file) {
        return fileConfig.postPath() + "/" + this.generateFileName(file);
    }

    // 디렉토리 생성 로직 내장
    private boolean makeDir(Path targetDir) {
        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            return true;
        } catch (IOException | IllegalStateException e) {
            return false;
        }
    }

    // 파일 저장
    public void saveFile(MultipartFile file, String logicalPath) {
        try {
            // 실제 물리적인 절대 경로 합성 (OS 구분자 자동 보정)
            Path physicalPath = Paths.get(fileConfig.storagePath(), logicalPath).normalize();

            // 부모 디렉토리 추출 후 자동 생성
            if(!this.makeDir(physicalPath.getParent())) {
                throw new FileStorageException(String.format("파일 저장 실패: 디렉토리를 생성할 수 없습니다. 경로: %s", physicalPath.getParent()));
            }

            // 파일 저장
            file.transferTo(physicalPath.toFile());
        } catch (IOException | IllegalStateException e) {
            throw new FileStorageException(String.format("파일 저장 실패: 쓰기 작업 중 에러가 발생했습니다. 파일명: %s", logicalPath), e);
        }
    }

    // 논리 경로 -> 실제 물리 경로 변환 (보안을 위해 상위 경로 탈출 방지 normalize 적용)
    private Path convertLogicalPathToPhysicalPath(String logicalPath) {
        String cleanPath = logicalPath.replace(fileConfig.serverUri(), "");
        return Paths.get(fileConfig.storagePath(), cleanPath).normalize();
    }

    // 삭제 성공 여부를 상태(boolean)로 반환하여 서비스에 제어권 양도 🎯
    public boolean destroyFile(String logicalPath) {
        // null, "", " " 체크
        if (!Strings.hasText(logicalPath)) {
            return false;
        }

        Path physical = convertLogicalPathToPhysicalPath(logicalPath);

        try {
            // 파일이 존재하면 삭제하고 true 반환, 없으면 false 반환
            return Files.deleteIfExists(physical);
        } catch (IOException | IllegalStateException e) {
            // 삭제 실패 시 에러를 터트려 시스템을 멈추기보다, false를 던져 서비스 레이어가 알게 함
            return false;
        }
    }
}
