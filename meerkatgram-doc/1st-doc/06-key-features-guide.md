# 06. 핵심 기능 구현 가이드

## 1. 글로벌 응답 처리

### 1-1. `GlobalRes<T>` — 공통 응답 DTO

```java
// global/responses/GlobalRes.java
@Getter
@Builder
public class GlobalRes<T> {
    private String code;
    private String message;
    private T data;
}
```

- `@Builder`: `.builder()...build()` 패턴으로 생성
- 제네릭 `<T>`로 어떤 타입의 데이터든 담을 수 있다
- `data`가 없을 땐 `null`이 들어간다 (Builder 기본값)

**사용 패턴 (Controller)**

```java
// 데이터 있는 응답
return ResponseEntity.status(200).body(
    GlobalRes.<UserRes>builder()
        .code("00")
        .message("정상 처리")
        .data(result)       // UserRes 객체
        .build()
);

// 데이터 없는 응답 (로그아웃 등)
return ResponseEntity.status(200).body(
    GlobalRes.<String>builder()
        .code("00")
        .message("정상 처리")
        .build()            // data는 null
);
```

---

### 1-2. `GlobalExceptionHandler` — `@RestControllerAdvice` 에러 처리

```java
// global/errors/GlobalExceptionHandler.java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 커스텀 예외: 로그인 실패
    @ExceptionHandler(NotRegisteredException.class)
    public ResponseEntity<GlobalRes<String>> authenticationHandle(NotRegisteredException e) {
        return ResponseEntity.status(401).body(
            GlobalRes.<String>builder().code("E01").message("로그인 에러").data(e.getMessage()).build()
        );
    }

    // @Valid 검증 실패: 여러 오류를 List로 반환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalRes<List<String>>> methodArgumentNotValidHandle(MethodArgumentNotValidException e) {
        return ResponseEntity.status(400).body(
            GlobalRes.<List<String>>builder()
                .code("E21")
                .message("요청 파라미터에 이상이 있습니다.")
                .data(
                    e.getBindingResult().getAllErrors().stream()
                        .map(item -> String.format("%s : 잘못된 값입니다.", item.getObjectName()))
                        .toList()
                )
                .build()
        );
    }

    // 파일 저장 실패: 스택 트레이스 로그 남김
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<GlobalRes<String>> fileStorageHandle(FileStorageException e) {
        log.error("파일 생성 에러: {}\n{}", e.getMessage(), Arrays.toString(e.getStackTrace()));
        return ResponseEntity.status(400).body(
            GlobalRes.<String>builder().code("E30").message("파일 생성 에러").data(e.getMessage()).build()
        );
    }

    // DB 에러: 클라이언트에 내부 정보 노출 방지
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
}
```

**`@RestControllerAdvice` 동작 원리**

```
Controller에서 예외 발생
    │
    ▼
스프링이 예외 타입에 맞는 @ExceptionHandler 탐색
    │
    ├─ NotRegisteredException  → authenticationHandle()
    ├─ FileStorageException    → fileStorageHandle()
    ├─ SQLException            → sqlExceptionHandle()
    └─ Exception (catch-all)   → othersHandle()
    │
    ▼
GlobalRes로 포장해서 HTTP 응답 반환
```

> `@ExceptionHandler`는 선언 순서와 관계없이 **가장 구체적인 타입**이 먼저 매칭된다.
> `Exception`은 모든 예외의 최상위 타입이므로 항상 마지막에 매칭된다.

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

**`FileStorageException`** — 파일 저장 실패

```java
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause); // 원인 예외도 함께 전달
    }
}
```

> 모두 `RuntimeException`을 상속한다. Checked Exception(`Exception` 직접 상속)이 아니므로 메서드 시그니처에 `throws` 선언이 필요 없다.

---

## 2. 파일 업로드

### 2-1. 파일 업로드 전체 흐름

```
Client (multipart/form-data)
    │
    ▼
FileController → FileService → LocalFileManager
    │                               │
    │                               ├─ 1. 확장자 검증 (jpg, png, webp만 허용)
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
public class LocalFileManager {
    private final List<String> ALLOW_EXTENTION_LIST =
        List.of("image/jpg", "image/jpeg", "image/png", "image/webp");

    // ① 확장자 검증
    public String getExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("파일 저장 실패: 파일 없음");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            throw new FileStorageException("파일 저장 실패: 파일명 이상");
        }

        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        if (!ALLOW_EXTENTION_LIST.contains("image/" + ext)) {
            throw new FileStorageException(
                String.format("파일 저장 실패: 허용하지 않는 확장자(.%s)", ext)
            );
        }
        return ext;
    }

    // ② 파일명 생성: 날짜_UUID.확장자 형식
    private String generateFileName(MultipartFile file) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return date + "_" + UUID.randomUUID() + "." + this.getExtension(file);
    }

    // ③ 논리 경로 생성 (게시글/프로필 구분)
    public String generatePostPath(MultipartFile file) {
        return fileConfig.postPath() + "/" + this.generateFileName(file);
    }
    public String generateProfilePath(MultipartFile file) {
        return fileConfig.profilePath() + "/" + this.generateFileName(file);
    }

    // ④ 파일 실제 저장
    public void saveFile(MultipartFile file, String logicalPath) {
        try {
            // 논리 경로 + storagePath → 실제 OS 경로 합성
            Path physicalPath = Paths.get(fileConfig.storagePath(), logicalPath).normalize();

            // 디렉토리 자동 생성
            if (!this.makeDir(physicalPath.getParent())) {
                throw new FileStorageException("디렉토리를 생성할 수 없습니다.");
            }

            file.transferTo(physicalPath.toFile()); // 실제 저장
        } catch (IOException e) {
            throw new FileStorageException("쓰기 작업 중 에러 발생: " + logicalPath, e);
        }
    }

    // ⑤ 파일 삭제 (실패해도 예외 없이 boolean 반환)
    public boolean destroyFile(String logicalPath) {
        if (!Strings.hasText(logicalPath)) return false;

        Path physical = convertLogicalPathToPhysicalPath(logicalPath);
        try {
            return Files.deleteIfExists(physical);
        } catch (IOException e) {
            return false; // 삭제 실패를 예외가 아닌 false로 처리 → 호출자가 판단
        }
    }
}
```

**파일 경로 구조 예시**

```
storagePath:  /storage
profilePath:  /images/profiles
postPath:     /images/posts
serverUri:    http://localhost:8080

게시글 이미지 저장 경로:
  물리 경로: /storage/images/posts/20250101_uuid.jpg
  논리 URL:  http://localhost:8080/images/posts/20250101_uuid.jpg
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

    public FileRes storePostImage(MultipartFile file) {
        String path = localFileManager.generatePostPath(file); // 논리 경로 생성
        localFileManager.saveFile(file, path);                 // 저장
        return FileRes.builder()
            .fileUrl(fileConfig.serverUri() + path)            // 접근 URL 반환
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
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /images/** URL로 오는 요청 → 실제 storagePath/images/ 폴더에서 파일 서빙
        String resourceLocation = Paths.get(fileConfig.storagePath() + "/images").toUri().toString();
        registry.addResourceHandler("/images/**")
                .addResourceLocations(resourceLocation);
    }
}
```

> `http://localhost:8080/images/posts/20250101_uuid.jpg` 요청이 오면,
> Spring이 `{storagePath}/images/posts/20250101_uuid.jpg` 파일을 읽어서 반환한다.

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

    // ② 해당 페이지 게시글 조회
    List<Post> posts = postMapper.getPagination(postIndexReq.limit(), offset);

    // ③ 전체 게시글 수로 마지막 페이지 여부 판단
    long total = postMapper.getTotal();
    boolean lastPage = offset + postIndexReq.limit() >= total;

    return PostIndexRes.builder()
        .total(total)
        .lastPage(lastPage)
        .posts(posts)
        .build();
}
```

```xml
<!-- PostMapper.xml -->
<select id="getPagination" resultMap="PostResultMap">
    SELECT * FROM posts
    WHERE deleted_at IS NULL
    ORDER BY created_at DESC, id
    LIMIT #{offset}, #{limit}   <!-- LIMIT {시작위치}, {개수} -->
</select>

<select id="getTotal">
    SELECT COUNT(*) FROM posts WHERE deleted_at IS NULL
</select>
```

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
public Post store(long userId, PostCreateReq postCreateReq) {
    // ① Builder로 Entity 생성
    Post post = Post.builder()
        .userId(userId)              // JWT Claims에서 추출한 userId
        .content(postCreateReq.content())
        .image(postCreateReq.image())
        .build();

    // ② INSERT 실행 (useGeneratedKeys로 id 자동 주입)
    postMapper.create(post);

    // ③ 방금 저장된 게시글을 JOIN으로 다시 조회해서 반환
    return postMapper.findByPk(post.getId());
}
```

```java
// PostController.java
@PostMapping("/posts")
public ResponseEntity<GlobalRes<Post>> store(
    @AuthenticationPrincipal Claims claims,    // SecurityContext에서 꺼냄
    @RequestBody PostCreateReq postCreateReq
) {
    // claims.getSubject() = "1" (userId를 String으로 저장했으므로 파싱 필요)
    Post result = postService.store(Long.parseLong(claims.getSubject()), postCreateReq);
    ...
}
```

---

### 3-3. 게시글 삭제 (소프트 삭제 + 파일 삭제)

```java
// PostService.java
@Transactional(rollbackFor = Exception.class)
public void destroy(long id) {
    // ① 삭제 대상 조회 (이미지 경로 확보 목적)
    Post post = postMapper.findByPk(id);
    if (post == null) {
        throw new RuntimeException("삭제 대상 게시글 없음");
    }

    // ② DB 소프트 삭제 (deleted_at = NOW())
    int cnt = postMapper.destroy(id);
    if (cnt != 1) {
        throw new RuntimeException("게시글 삭제 이상 발생");
    }

    // ③ 연결 이미지 파일 삭제 (실패해도 트랜잭션 롤백 없이 계속 진행)
    localFileManager.destroyFile(post.getImage());
}
```

> `destroyFile()`이 `false`를 반환해도 `@Transactional`이 롤백되지 않는다.
> 파일 삭제 실패는 치명적이지 않으므로(나중에 배치 정리 가능), DB 삭제만 보장하는 설계다.

---

## 4. MyBatis Mapper XML 작성 패턴

### 4-1. ResultMap — DB 컬럼 ↔ Java 필드 매핑

```xml
<resultMap id="PostResultMap" type="com.msa4meerkatgram.domain.post.entities.Post">
    <id     column="id"         property="id" />       <!-- PK는 <id> 태그 -->
    <result column="user_id"    property="userId" />   <!-- snake_case → camelCase -->
    <result column="content"    property="content" />
    <result column="image"      property="image" />
    <result column="created_at" property="createdAt" />
    <result column="updated_at" property="updatedAt" />
    <result column="deleted_at" property="deletedAt" />
</resultMap>
```

### 4-2. SELECT — resultMap 적용

```xml
<select id="findByPk" resultMap="PostResultMap">
    SELECT
        posts.*
        , users.nick
        , users.email
        , users.profile
    FROM posts
    JOIN users ON posts.user_id = users.id
    WHERE posts.deleted_at IS NULL
      AND posts.id = #{id}
</select>
```

> `#{id}` — MyBatis의 파라미터 바인딩. `?` PreparedStatement로 변환되어 SQL 인젝션을 방지한다.

### 4-3. INSERT — `useGeneratedKeys`

```xml
<insert
    id="create"
    parameterType="com.msa4meerkatgram.domain.post.entities.Post"
    useGeneratedKeys="true"
    keyProperty="id"
>
    INSERT INTO posts (user_id, content, image, created_at, updated_at)
    VALUES (#{userId}, #{content}, #{image}, NOW(), NOW())
</insert>
```

### 4-4. UPDATE — 소프트 삭제

```xml
<update id="destroy">
    UPDATE posts
    SET
        updated_at = NOW()
      , deleted_at = NOW()
    WHERE id = #{id}
</update>
```

### XML 속성 정리

| 속성 | 설명 |
|------|------|
| `namespace` | 연결할 Mapper 인터페이스의 전체 경로 |
| `id` | Mapper 인터페이스의 메서드명과 일치해야 함 |
| `resultMap` | 결과를 매핑할 `<resultMap>` id |
| `parameterType` | 파라미터 타입 (단일 객체일 때 명시, 단순 타입은 생략 가능) |
| `useGeneratedKeys` | AUTO_INCREMENT PK 자동 주입 여부 |
| `keyProperty` | PK를 주입할 Java 객체의 필드명 |
