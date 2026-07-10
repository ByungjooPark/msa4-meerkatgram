# 06. 핵심 기능 구현 가이드

## 1. 글로벌 응답 처리

### 1-1. `GlobalRes<T>` — 공통 응답 DTO

```java
// global/responses/GlobalRes.java — record (Builder 클래스 아님)
public record GlobalRes<T>(
    String code
    , String message
    , T data
) {
    public static <T> GlobalRes<T> from(CustomResponseCode customResponseCode, T data) {
        return new GlobalRes<T>(customResponseCode.getCode(), customResponseCode.name(), data);
    }

    public static GlobalRes<Void> from(CustomResponseCode customResponseCode) {
        return new GlobalRes<Void>(customResponseCode.getCode(), customResponseCode.name(), null);
    }

    public static <T> GlobalRes<T> success(T data) {
        return GlobalRes.<T>from(CustomResponseCode.SUCCESS, data);
    }

    public static GlobalRes<Void> success() {
        return GlobalRes.<Void>from(CustomResponseCode.SUCCESS);
    }
}
```

- record이므로 `.builder()...build()`가 아니라 정적 팩토리 메서드(`from`, `success`)로 생성한다
- 제네릭 `<T>`로 어떤 타입의 데이터든 담을 수 있다
- `message`에는 `CustomResponseCode` enum 상수명이 그대로 들어간다(`"SUCCESS"`, `"NOT_REGISTERED_ERROR"` 등). 한글 문장이 아니다
- `success()`(인자 없음) 호출 시 `data`는 `null`이 들어간다

**사용 패턴 (Controller)**

```java
// 데이터 있는 응답
return ResponseEntity.ok(GlobalRes.success(result));

// 데이터 없는 응답 (로그아웃, 회원가입 등)
return ResponseEntity.ok(GlobalRes.success());
```

---

### 1-2. `GlobalExceptionHandler` — `@RestControllerAdvice` 에러 처리

```java
// global/errors/GlobalExceptionHandler.java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 예외 → 에러코드 매핑 후 공통 응답 생성을 담당하는 private 헬퍼
    private ResponseEntity<GlobalRes<Void>> generateErrorResponse(CustomResponseCode customResponseCode) {
        return ResponseEntity.status(customResponseCode.getHttpStatus())
            .body(GlobalRes.<Void>from(customResponseCode));
    }

    // 커스텀 예외: 로그인 실패
    @ExceptionHandler(NotRegisteredException.class)
    public ResponseEntity<GlobalRes<Void>> notRegisteredHandle(NotRegisteredException e) {
        log.debug(CustomResponseCode.NOT_REGISTERED_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.NOT_REGISTERED_ERROR);
    }

    // @Valid 검증 실패: 필드별 오류는 로깅만 하고, 응답 data는 null로 통일
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalRes<Void>> methodArgumentNotValidHandle(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, fe -> fe.getDefaultMessage(), (a, b) -> a));
        log.debug(CustomResponseCode.INVALID_PARAMETER_ERROR.name(), errors);
        return this.generateErrorResponse(CustomResponseCode.INVALID_PARAMETER_ERROR);
    }

    // 파일 저장/삭제 실패
    @ExceptionHandler(FileManagedException.class)
    public ResponseEntity<GlobalRes<Void>> fileManagedHandle(FileManagedException e) {
        log.debug(CustomResponseCode.FILE_MANAGED_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.FILE_MANAGED_ERROR);
    }

    // 존재하지 않는 URL 요청
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GlobalRes<Void>> notFoundHandle(NoResourceFoundException e) {
        return this.generateErrorResponse(CustomResponseCode.NOT_FOUND_ERROR);
    }

    // DB 에러: 클라이언트에 내부 정보 노출 방지
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
```

**`@RestControllerAdvice` 동작 원리**

```
Controller에서 예외 발생
    │
    ▼
스프링이 예외 타입에 맞는 @ExceptionHandler 탐색
    │
    ├─ NotRegisteredException     → notRegisteredHandle()
    ├─ FileManagedException       → fileManagedHandle()
    ├─ NoResourceFoundException   → notFoundHandle()
    ├─ SQLException                → sqlHandle()
    └─ Exception (catch-all)      → othersHandle()
    │
    ▼
GlobalRes<Void>(data: null)로 포장해서 HTTP 응답 반환
```

> `@ExceptionHandler`는 선언 순서와 관계없이 **가장 구체적인 타입**이 먼저 매칭된다.
> `Exception`은 모든 예외의 최상위 타입이므로 항상 마지막에 매칭된다.
> 모든 핸들러가 예외 상세 메시지를 응답이 아니라 로그로만 남긴다 — 클라이언트에 내부 정보를 노출하지 않기 위함이다.

---

### 1-3. 커스텀 예외 클래스 구조

커스텀 예외는 `global/errors/custom/` 에 위치한다.

**`NotRegisteredException`** — 로그인 인증 실패

```java
public class NotRegisteredException extends RuntimeException {
    public NotRegisteredException(String message) {
        super(message);
    }
}
```

**`InvalidTokenException`** — JWT 토큰 오류

```java
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
```

**`DeletedRecordException`** — 조회 대상이 이미 삭제됨/존재하지 않음

```java
public class DeletedRecordException extends RuntimeException {
    public DeletedRecordException(String message) {
        super(message);
    }
}
```

**`DuplicatedRecordException`** — 이메일 등 중복 데이터

```java
public class DuplicatedRecordException extends RuntimeException {
    public DuplicatedRecordException(String message) {
        super(message);
    }
}
```

**`FileManagedException`** — 파일 저장/삭제 실패

```java
public class FileManagedException extends RuntimeException {
    public FileManagedException(String message) {
        super(message);
    }
}
```

> 모두 `RuntimeException`을 상속한다. Checked Exception(`Exception` 직접 상속)이 아니므로 메서드 시그니처에 `throws` 선언이 필요 없다.
> `global/errors/custom/`에는 `DuplicatedUserException`도 있지만, 실제로는 어디에서도 사용되지 않는 죽은 코드다(핸들러도 없음).

---

## 2. 파일 업로드

### 2-1. 파일 업로드 전체 흐름

```
Client (multipart/form-data)
    │
    ▼
FileController → FileService → LocalFileManager
    │                               │
    │                               ├─ 1. 확장자 검증 (jpg, jpeg, png, gif, svg, webp만 허용)
    │                               ├─ 2. 파일명 생성 (날짜 + UUID)
    │                               ├─ 3. 저장 경로(논리) 생성
    │                               └─ 4. 실제 디스크에 파일 저장
    │
    ▼
fileConfig.serverUri() + 논리 경로 → URL 반환
```

---

### 2-2. `LocalFileManager` — 파일 저장 유틸

```java
// global/util/file/LocalFileManager.java
@Component
@RequiredArgsConstructor
public class LocalFileManager {
    private final FileConfig fileConfig;

    // ① 확장자 추출 및 검증
    public String extractExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileManagedException("파일 저장 실패: 파일 확장자 획득 실패(파일 없음)");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            throw new FileManagedException("파일 저장 실패: 파일 확장자 획득 실패(파일명 이상)");
        }
        String extractExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        if (!fileConfig.allowExtensionList().contains("image/" + extractExtension)) {
            throw new FileManagedException("파일 저장 실패: 허용하지 않는 파일 확장자");
        }
        return extractExtension;
    }

    // ② 파일명 생성: `yyyyMMdd_UUID` (확장자 미포함)
    public String generateFileName() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return LocalDate.now().format(dateFormatter) + "_" + UUID.randomUUID();
    }

    // ③ 논리 경로 생성 (게시글/프로필 구분, 파일명 + 확장자 조합)
    public String generateProfilePath(MultipartFile file) {
        return fileConfig.profilePath() + "/" + generateFileName() + "." + extractExtension(file);
    }
    public String generatePostPath(MultipartFile file) {
        return fileConfig.postPath() + "/" + generateFileName() + "." + extractExtension(file);
    }

    // ④ 디렉토리 생성 (없을 때만)
    public boolean makeDir(Path targetPath) {
        try {
            if (!Files.exists(targetPath)) {
                Files.createDirectories(targetPath);
            }
            return true;
        } catch (IOException | IllegalStateException e) {
            return false;
        }
    }

    // ⑤ 파일 실제 저장
    public void saveFile(MultipartFile file, String logicalPath) {
        try {
            // 논리 경로 + storagePath → 실제 OS 경로 합성(OS 구분자 자동 보정)
            Path physicalPath = Paths.get(fileConfig.storagePath(), logicalPath).normalize();

            if (!this.makeDir(physicalPath.getParent())) {
                throw new FileManagedException(String.format("파일 저장 실패: 디렉토리 생성 실패 (경로: %s)", physicalPath.getParent()));
            }

            file.transferTo(physicalPath.toFile());
        } catch (IOException | IllegalStateException e) {
            throw new FileManagedException(String.format("파일 저장 실패: 쓰기 작업 실패 (파일명: %s)", logicalPath));
        }
    }
}
```

**파일 경로 구조 예시**

```
storagePath:  /storage
profilePath:  /files/profiles
postPath:     /files/posts
serverUri:    http://localhost:8080

게시글 이미지 저장 경로:
  물리 경로: /storage/files/posts/20250101_uuid.jpg
  논리 URL:  http://localhost:8080/files/posts/20250101_uuid.jpg
```

> 물리 경로(서버 디렉토리)와 논리 URL을 분리해서 관리한다.
> 클라이언트는 논리 URL만 알면 되고, 실제 저장 경로는 서버 내부 정보로 숨긴다.

---

### 2-3. `FileService` — 서비스 레이어

```java
// domain/file/services/FileService.java
@Service
@RequiredArgsConstructor
public class FileService {
    private final LocalFileManager localFileManager;
    private final FileConfig fileConfig;

    public FileRes storeProfile(MultipartFile file) {
        String path = localFileManager.generateProfilePath(file); // 논리 경로 생성
        localFileManager.saveFile(file, path);                    // 저장
        return FileRes.builder()
            .fileUri(fileConfig.serverUri() + path)                // 접근 URL 반환
            .build();
    }

    public FileRes storePosts(MultipartFile file) {
        String path = localFileManager.generatePostPath(file);
        localFileManager.saveFile(file, path);
        return FileRes.builder()
            .fileUri(fileConfig.serverUri() + path)
            .build();
    }
}
```

---

### 2-4. `WebConfig` — 정적 리소스 URL 매핑

업로드한 이미지를 브라우저에서 URL로 접근하려면, Spring MVC에 정적 리소스 경로를 등록해야 한다.

```java
// global/config/WebConfig.java
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final FileConfig fileConfig;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /files/** URL로 오는 요청 → 실제 storagePath/files/ 폴더에서 파일 서빙
        String resourceLocation = Paths.get(fileConfig.storagePath() + "/files").toUri().toString();
        registry.addResourceHandler("/files/**")
                .addResourceLocations(resourceLocation);
    }
}
```

> `http://localhost:8080/files/posts/20250101_uuid.jpg` 요청이 오면,
> Spring이 `{storagePath}/files/posts/20250101_uuid.jpg` 파일을 읽어서 반환한다.

---

### 2-5. CORS 설정 (`CorsConfig` + `SecurityConfiguration`)

CORS는 브라우저가 다른 출처(Origin)의 API를 호출할 때 적용되는 보안 정책이다.
프론트(Vue, `localhost:5173`)에서 백엔드(`localhost:8080`)를 호출하면 브라우저가 CORS 정책을 적용한다.

```java
// global/config/CorsConfig.java (@ConfigurationProperties)
public record CorsConfig(
    List<String> allowedOrigins, // 허용할 프론트엔드 도메인 목록
    Long maxAge                  // Preflight 요청 결과 캐싱 시간 (초)
) {}
```

```java
// SecurityConfiguration.java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(corsConfig.allowedOrigins()); // e.g. ["http://localhost:5173"]
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    configuration.setAllowCredentials(true); // 쿠키(Refresh Token) 전송 허용
    configuration.setMaxAge(corsConfig.maxAge());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

> `setAllowCredentials(true)` 설정이 필요한 이유:
> Refresh Token을 HttpOnly 쿠키로 전송하는데, 쿠키는 `credentials`에 해당한다.
> 이 설정이 없으면 브라우저가 쿠키를 차단한다.
> 단, `allowCredentials(true)` 사용 시 `allowedOrigins`에 `"*"` 와일드카드를 사용할 수 없다.

---

## 3. 게시글 CRUD

### 3-1. 게시글 목록 — 페이지네이션

```java
// PostService.java
public PostIndexRes index(PostIndexReq postIndexReq) {
    // ① offset 계산: 3페이지, limit 6 → offset = (3-1) * 6 = 12
    int offset = (postIndexReq.page() - 1) * postIndexReq.limit();

    // ② 해당 페이지 게시글 조회 (QueryDSL)
    List<Post> result = postQueryRepository.pagination(offset, postIndexReq.limit());

    // ③ 전체 게시글 수로 마지막 페이지 여부 판단 (Spring Data JPA)
    long total = postRepository.count();
    boolean lastPage = offset + postIndexReq.limit() >= total;

    return PostIndexRes.from(total, lastPage, result);
}
```

```java
// PostQueryRepository.java — Spring Data 인터페이스가 아니라 @Repository 클래스로, JPAQueryFactory를 직접 사용한다
@Repository
@RequiredArgsConstructor
public class PostQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;

    public List<Post> pagination(int offset, int limit) {
        QPost post = QPost.post;
        QUser user = QUser.user;

        return jpaQueryFactory
            .selectFrom(post)
            .join(post.user, user).fetchJoin()   // N+1 방지: 작성자 정보를 한 번에 조인 조회
            .orderBy(post.createdAt.desc(), post.id.desc())
            .limit(limit)
            .offset(offset)
            .fetch();
    }
}
```

- `@SQLRestriction("deleted_at IS NULL")`이 `Post` 엔티티에 붙어 있으므로, 별도 조건 없이도 소프트 삭제된 게시글은 결과에서 자동 제외된다
- `fetchJoin()`: LAZY로 설정된 `post.user` 연관관계를 SELECT 시점에 함께 조회한다(작성자 정보를 나중에 따로 조회하지 않아도 됨)

**페이지네이션 계산 예시**

| page | limit | offset | 조회 범위 |
|------|-------|--------|----------|
| 1 | 6 | 0 | 1~6번째 |
| 2 | 6 | 6 | 7~12번째 |
| 3 | 6 | 12 | 13~18번째 |

**`PostIndexReq`의 기본값 처리**

```java
public record PostIndexReq(Integer page, Integer limit) {
    public PostIndexReq(Integer page, Integer limit) {
        // page, limit 미전송 시 기본값 적용
        this.page  = (page  != null && page  > 0) ? page  : 1;
        this.limit = (limit != null && limit > 0) ? limit : 6;
    }
}
```

---

### 3-2. 게시글 작성

```java
// PostService.java
@Transactional(rollbackFor = Exception.class)
public PostWithUserRes store(long userId, PostStoreReq postStoreReq) {
    // ① Entity 생성 및 값 세팅
    Post post = new Post();
    post.setContent(postStoreReq.content());
    post.setImage(postStoreReq.image());
    post.setUser(userRepository.getReferenceById(userId));   // 프록시만 걸고, 별도 SELECT는 실행하지 않는다

    // ② 저장 후 응답 DTO로 변환해서 반환
    return PostWithUserRes.from(postRepository.save(post));
}
```

```java
// PostController.java
@PostMapping("/posts")
public ResponseEntity<GlobalRes<PostWithUserRes>> store(
    @Valid @RequestBody PostStoreReq postStoreReq
    , @AuthenticationPrincipal Claims claims    // SecurityContext에서 꺼냄
) {
    // claims.getSubject() = "1" (userId를 String으로 저장했으므로 파싱 필요)
    return ResponseEntity.ok(GlobalRes.success(postService.store(Long.parseLong(claims.getSubject()), postStoreReq)));
}
```

> `userRepository.getReferenceById(userId)`는 실제 SELECT 없이 프록시(참조)만 반환한다.
> `Post.user`는 `@JoinColumn(insertable = true, updatable = false, ...)`이므로, INSERT 시에는 `user_id` 컬럼이
> 함께 기록되지만 이후 게시글의 작성자를 변경하는 UPDATE는 발생하지 않는다.

---

### 3-3. 게시글 삭제 — ⚠️ 현재 미구현

`PostController`에는 삭제 관련 메서드 자체가 없다(주석 처리조차 되어 있지 않음). 아래는 재구현 시 참고할 설계안이다.

```java
// PostService.java (설계안 — 실제 코드 아님)
@Transactional(rollbackFor = Exception.class)
public void destroy(long id) {
    Post post = postRepository.findById(id)
        .orElseThrow(() -> new DeletedRecordException("이미 삭제된 게시글입니다."));

    // repository.delete()를 호출해도 Post 엔티티의 @SQLDelete 덕분에
    // 실제로는 물리 DELETE가 아니라 `UPDATE posts SET deleted_at = NOW() ...`가 실행된다
    postRepository.delete(post);

    // 연결 이미지 파일도 함께 정리하려면 LocalFileManager에 파일 삭제 메서드를 추가해야 한다
    // (현재 LocalFileManager에는 저장(saveFile) 관련 메서드만 있고 삭제 메서드는 없다)
}
```

> 소프트 삭제 자체는 `@SQLDelete`/`@SQLRestriction`이 자동으로 처리해 주므로, MyBatis 시절처럼
> "삭제 처리 SQL"과 "조회 시 제외 조건"을 직접 작성할 필요는 없다. 남은 작업은 컨트롤러/서비스 메서드
> 추가와 파일 정리 로직뿐이다.

---

## 4. Spring Data JPA / QueryDSL 리포지토리 작성 패턴

### 4-1. Spring Data JPA — 메서드 이름 기반 쿼리

```java
// PostRepository.java
public interface PostRepository extends JpaRepository<Post, Long> {
    long countByUser(User user);
}
```

- `JpaRepository<Post, Long>`을 상속하면 `save()`, `findById()`, `delete()`, `count()` 등 기본 CRUD를 별도 구현 없이 사용할 수 있다
- `countByUser`처럼 메서드 이름을 `동사 + By + 필드명` 규칙으로 지으면, Spring Data JPA가 이름을 분석해 쿼리를 자동 생성한다(쿼리 메서드) — SQL을 직접 작성하지 않는다

### 4-2. QueryDSL — 동적/복잡 조회

```java
// PostQueryRepository.java
@Repository
@RequiredArgsConstructor
public class PostQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;

    public List<Post> pagination(int offset, int limit) {
        QPost post = QPost.post;
        QUser user = QUser.user;

        return jpaQueryFactory
            .selectFrom(post)
            .join(post.user, user).fetchJoin()
            .orderBy(post.createdAt.desc(), post.id.desc())
            .limit(limit)
            .offset(offset)
            .fetch();
    }
}
```

- Spring Data 인터페이스가 아니라 일반 `@Repository` 클래스로, 생성자로 주입받은 `JPAQueryFactory`(`global/config/jpa/QueryDSLConfig.java`에서 빈으로 등록)를 직접 사용한다
- `QPost`, `QUser`는 QueryDSL APT가 엔티티(`@Entity`)를 기반으로 컴파일 시점에 자동 생성하는 클래스다(`build/generated/...`에 생성됨)
- 페이지네이션, 다중 조건 검색처럼 메서드 이름 규칙만으로는 표현하기 어려운 동적/복잡 쿼리를 여기서 작성한다

### 속성/개념 정리

| 개념 | 설명 |
|------|------|
| `JpaRepository<Entity, ID>` | 기본 CRUD 메서드를 제공하는 Spring Data JPA 최상위 인터페이스 |
| 쿼리 메서드 | `findByEmail`, `countByUser`처럼 메서드 이름 규칙으로 쿼리를 자동 생성 |
| `JPAQueryFactory` | QueryDSL로 타입-세이프한 쿼리를 작성하기 위한 진입점 |
| `fetchJoin()` | LAZY 연관관계를 조회 시점에 함께 가져와 N+1 문제를 방지 |
| `@GeneratedValue(strategy = IDENTITY)` | AUTO_INCREMENT PK를 `save()` 직후 엔티티에 자동 주입 |
