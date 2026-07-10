# 현재 코드베이스 조사 보고서 (feature/v2/migration-jpa 기준)

> 이 문서는 `doc/1st-doc/` (구 버전 문서)와 실제 코드 사이의 차이를 파악하기 위한 조사 결과다.
> 최신 커밋(`0205856 add doc AI test`) 기준으로 작성했다.
> 문서 수정 작업의 근거 자료로 사용하며, 이 문서 자체는 조사 기록이지 최종 문서가 아니다.

## 핵심 요약

MyBatis → **Spring Data JPA + QueryDSL**로 전환 완료. 다만 `doc/1st-doc`의 "v2 예정"에서 예고했던
`comments`/`likes`/`notifications`/`push_subscriptions` 도메인은 아직 코드에 존재하지 않는다.
여전히 `user`/`post`/`auth`/`file` 4개 도메인만 존재한다.

---

## 1. 기술 스택 / 빌드 설정

`build.gradle` 기준:

- Java 17 toolchain
- **Spring Boot 3.5.15-SNAPSHOT** (`repo.spring.io/snapshot`에서 받아옴 — 정식 GA 릴리즈 아님)
- `spring-boot-starter-jdbc`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-web`
- `spring-boot-starter-data-jpa` — MyBatis 완전 대체 (mybatis 의존성 없음)
- QueryDSL: `com.querydsl:querydsl-jpa:5.1.0:jakarta` + `querydsl-apt:5.1.0:jakarta` (Jakarta 버전, `Q*` 클래스는 `build/generated/...`에 생성)
- `io.jsonwebtoken:jjwt:0.12.6`
- `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.16` — **신규 추가** (커밋: `9441e0e add Springdoc OpenApi`, `e509512`, `63be1b4`)
- Lombok, `com.mysql:mysql-connector-j` (runtimeOnly)
- `spring-boot-configuration-processor`
- 테스트: `spring-boot-starter-test`, `spring-security-test`, JUnit Platform Launcher

**Docker**: `Dockerfile` (루트) — `gradle:8-jdk17-alpine` 빌더 → `eclipse-temurin:17-jre` 런타임, `bootJar -x test`, 8080 포트 노출. **docker-compose.yml은 없음.**

**CI/CD**: `Jenkinsfile` (루트) — Docker 이미지 빌드/푸시(사설 레지스트리 `192.168.0.5:6901`) 후 별도 `k8s-manifests` 저장소의 `deployment.yaml`을 갱신·푸시하는 GitOps 방식. 구 문서에는 전혀 언급 없음.

Flyway/Liquibase 마이그레이션 없음.

---

## 2. 패키지 구조

루트 패키지: `com.msa4meerkatgram`

```
com.msa4meerkatgram
├── Msa4MeerkatgramApplication.java   (@SpringBootApplication, @ConfigurationPropertiesScan, @EnableJpaAuditing)
├── domain
│   ├── auth
│   │   ├── controllers/AuthController.java
│   │   ├── repositories/AuthRepository.java   (JpaRepository<User,Long>)
│   │   ├── requests/LoginReq.java, RegistrationReq.java
│   │   ├── responses/AuthRes.java
│   │   └── services/AuthService.java
│   │   (entities/ 디렉토리 존재하나 비어있음 — auth 전용 엔티티 없이 User 재사용)
│   ├── file
│   │   ├── controllers/FileController.java
│   │   ├── responses/FileRes.java
│   │   └── services/FileService.java
│   │   (entities/repositories/requests 없음 — DB 테이블 없는 무상태 도메인)
│   ├── post
│   │   ├── controllers/PostController.java
│   │   ├── entities/Post.java
│   │   ├── repositories/PostRepository.java (Spring Data JPA), PostQueryRepository.java (QueryDSL)
│   │   ├── requests/PostIndexReq.java, PostStoreReq.java (현재 미사용)
│   │   ├── responses/PostIndexRes.java, PostWithUserRes.java
│   │   └── services/PostService.java
│   └── user
│       ├── controllers/UserController.java   (빈 스텁 — 엔드포인트 없음)
│       ├── entities/User.java
│       ├── repositories/UserRepository.java  (커스텀 메서드 없음)
│       ├── responses/UserRes.java, UserWithPostCountRes.java
│       └── services/UserService.java         (빈 스텁 — JwtProvider만 주입, 미사용)
└── global
    ├── config
    │   ├── CorsConfig.java            (@ConfigurationProperties("cors"))
    │   ├── WebConfig.java             (/files/** 정적 리소스 핸들러)
    │   ├── jpa/QueryDSLConfig.java    (JPAQueryFactory 빈)
    │   └── openapi/OpenApiConfig.java, CustomApiResponse.java, ApiResponseCustomizer.java
    ├── errors
    │   ├── GlobalExceptionHandler.java (@RestControllerAdvice)
    │   └── custom/ (NotRegisteredException, InvalidTokenException, DeletedRecordException,
    │                FileManagedException, DuplicatedRecordException, DuplicatedUserException[미사용/dead])
    ├── responses
    │   ├── GlobalRes.java
    │   └── constant/CustomResponseCode.java
    ├── security
    │   ├── constant/ProviderPolicy.java, RolePolicy.java
    │   ├── cookie/CookieManager.java
    │   ├── filter/SecurityConfiguration.java, SecurityUrlRegistry.java, TokenAuthenticationFilter.java,
    │   │          SecurityAuthenticationProvider.java, SecurityExceptionHandler.java
    │   └── jwt/JwtProvider.java, JwtConfig.java
    └── util/file/FileConfig.java, LocalFileManager.java
```

**존재하는 도메인: `auth`, `file`, `post`, `user` 뿐.** `comment`/`like`/`notification`/`push_subscription`은 아직 코드에 없다.

---

## 3. 엔티티

JPA 엔티티는 `User`, `Post` 두 개뿐이다. `auth`는 `User`를 재사용하고, `file`은 엔티티가 없다.

### `User` (`domain/user/entities/User.java`)

- `@Entity`, `@EntityListeners(AuditingEntityListener.class)`, `@Table(name = "users")`
- `@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE id = ?")` + `@SQLRestriction("deleted_at IS NULL")` → Hibernate 6 소프트 삭제 메커니즘 (더 이상 MyBatis 수동 쿼리 아님)
- `@Getter @Setter` (Lombok)

| 필드 | 타입 | 어노테이션/설명 |
|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue(IDENTITY)`, `columnDefinition="BIGINT UNSIGNED"` |
| `email` | `String` | `unique=true, nullable=false, length=100` |
| `password` | `String` | `nullable=false, length=255` |
| `nick` | `String` | `nullable=false, length=20` — **unique 아님** (구 문서: `VARCHAR(15)` UK) |
| `provider` | `ProviderPolicy` | enum, `@Enumerated(STRING)` + `@JdbcTypeCode(VARCHAR)`, 기본값 `NONE` |
| `role` | `RolePolicy` | enum, 동일 방식, 기본값 `NORMAL` |
| `profile` | `String` | `nullable=false, length=100` |
| `refreshToken` | `String` | `nullable=true, length=255`, 컬럼명 `refresh_token` |
| `createdAt` | `LocalDateTime` | `@CreatedDate`, `nullable=false` |
| `updatedAt` | `LocalDateTime` | `@LastModifiedDate`, `nullable=false` |
| `deletedAt` | `LocalDateTime` | `nullable=true`, 소프트 삭제 마커 |

- `@OneToMany(mappedBy="user") List<Post> posts` 주석 처리됨 — 아직 비활성 (단방향 Post→User만 존재)

### `Post` (`domain/post/entities/Post.java`)

- `@Entity`, `@EntityListeners(AuditingEntityListener.class)`, `@Table(name = "posts")`
- 동일한 `@SQLDelete`/`@SQLRestriction` 패턴

| 필드 | 타입 | 어노테이션/설명 |
|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue(IDENTITY)`, `BIGINT UNSIGNED` |
| `content` | `String` | `nullable=false, length=200` |
| `image` | `String` | `nullable=false, length=100` — **구 문서와 달리 not-null** |
| `createdAt`/`updatedAt` | `LocalDateTime` | User와 동일 auditing 패턴 |
| `deletedAt` | `LocalDateTime` | nullable |
| `user` | `User` | `@ManyToOne(LAZY)` + `@JoinColumn(name="user_id", insertable=true, updatable=false, nullable=false, foreignKey=@ForeignKey(NO_CONSTRAINT))` → **DB에 물리적 FK 제약 없음** |

PK 타입: `Long` ↔ `BIGINT UNSIGNED` (`columnDefinition` 명시), `GenerationType.IDENTITY`.

Enum:
- `ProviderPolicy`: `NONE, KAKAO, GOOGLE`
- `RolePolicy`: `NORMAL, SUPER` (구 문서의 오타 `NOMAL` → 코드에서는 정상적으로 `NORMAL`)

관계: `Post.user` → `User` (다대일, LAZY, 역방향 컬렉션 비활성). Comment/Like/Notification/PushSubscription 엔티티 없음.

---

## 4. 리포지토리

모두 Spring Data JPA — **MyBatis Mapper XML은 전혀 존재하지 않음** (`src/main/resources/mapper` 디렉토리 자체가 없음, 전체 저장소에서 `.xml` 매퍼 0건 확인). MyBatis는 완전히 제거됨 (커밋 `ced0e10 JPA 전환작업중 (Mybatis 관련 코드 제거)`).

- `AuthRepository extends JpaRepository<User, Long>`
  - `Optional<User> findByEmail(String email)`
  - `boolean existsByEmail(String email)`
- `UserRepository extends JpaRepository<User, Long>` — 커스텀 메서드 없음, 현재 어떤 서비스에서도 사용 안 함
- `PostRepository extends JpaRepository<Post, Long>`
  - `long countByUser(User user)`
- `PostQueryRepository` — Spring Data 인터페이스가 아닌 `@Repository` 클래스, `JPAQueryFactory` 래핑 (QueryDSL)
  - `List<Post> pagination(int offset, int limit)` → `selectFrom(post).join(post.user, user).fetchJoin().orderBy(post.createdAt.desc(), post.id.desc()).limit(limit).offset(offset).fetch()`

> **주의(정리 대상)**: `application-prod.yaml`에 MyBatis가 더 이상 의존성에 없음에도 `mybatis:` 설정 블록(`mapper-locations: classpath:mapper/**/*Mapper.xml`)이 남아있다. 죽은 설정이며 문서화 대상이 아니다.

---

## 5. 컨트롤러 & 엔드포인트

모든 컨트롤러 base path는 `/api`. 응답은 항상 `GlobalRes<T>`로 감싸짐. 인증 필요 여부는 엔드포인트별 어노테이션이 아니라 `SecurityUrlRegistry`에서 중앙관리(§7 참고).

### AuthController (`/api`) — Tag: "인증 API"

| Method | Path | 인증 | Request | Response |
|---|---|---|---|---|
| POST | `/api/login` | 불필요 | `LoginReq` | `GlobalRes<AuthRes>` |
| POST | `/api/reissue-token` | 불필요(refresh 쿠키 사용) | 없음 | `GlobalRes<AuthRes>` |
| POST | `/api/logout` | **필요** | 없음 (`@AuthenticationPrincipal Claims`) | `GlobalRes<Void>` |
| POST | `/api/registration` | 불필요 | `RegistrationReq` | `GlobalRes<Void>` |

> 회원가입은 `POST /api/registration` (AuthController 소속)이다. 구 문서의 `POST /api/users`는 존재하지 않는다.

### PostController (`/api`) — Tag: "게시글 API"

| Method | Path | 인증 | Request | Response |
|---|---|---|---|---|
| GET | `/api/posts` | 불필요 | `PostIndexReq` (쿼리 파라미터) | `GlobalRes<PostIndexRes>` |
| GET | `/api/posts/{id}` | **필요** | path `id` (`@Min(1)`) | `GlobalRes<PostWithUserRes>` |
| POST | `/api/posts` | `SecurityUrlRegistry`상 필요로 등록돼 있으나 | **컨트롤러 메서드/서비스 로직 모두 주석처리 — 비활성 상태** |
| DELETE | `/api/posts/{id}` | `SecurityUrlRegistry`상 필요로 등록돼 있으나 | **컨트롤러에 해당 메서드 자체가 없음 — 미구현** |

> 게시글 작성(POST)과 삭제(DELETE)는 `SecurityUrlRegistry`에는 여전히 인증 필요 URL로 등록되어 있지만, 실제로는 죽은 코드이거나 아예 구현되지 않았다. `PostStoreReq` DTO는 존재하지만 사용되지 않는다.

페이지네이션: **offset 기반 커스텀 (`page`/`limit`), Spring Data `Pageable`/`Slice` 아님.** `PostIndexReq`가 compact constructor로 기본값(`page=1`, `limit=6`) 정규화. 서비스에서 `offset = (page-1)*limit` 계산 후 QueryDSL 조회, 별도 `count()` 쿼리로 `total` 계산, `lastPage = offset + limit >= total`.

### FileController (`/api`) — Tag: "파일 API"

| Method | Path | 인증 | Request | Response |
|---|---|---|---|---|
| POST | `/api/files/profiles` | 불필요 | `@ModelAttribute MultipartFile file` | `GlobalRes<FileRes>` |
| POST | `/api/files/posts` | 불필요 | `@ModelAttribute MultipartFile file` | `GlobalRes<FileRes>` |

> 경로가 `/api/files/profiles`, `/api/files/posts`로 바뀌었다 (구 문서: `/api/images/...`). 응답 필드도 `fileUri`이다 (구 문서: `fileUrl`).

### UserController (`/api`)

빈 컨트롤러. `UserService` 필드만 주입, **엔드포인트 0개**. `GET /api/users/{id}`는 존재하지 않는다 (구 문서 5-1과 모순). 유저 정보는 `AuthRes`/`PostWithUserRes` 내부에 `UserRes`/`UserWithPostCountRes`로 중첩되어서만 노출된다.

---

## 6. DTO / Request / Response

**Auth**
- `LoginReq`: `email`(`@NotBlank`, `@Pattern` 이메일 정규식), `password`(`@NotBlank`, `@Pattern` 영문+숫자+특수문자 8~20자). 각 필드에 `@Schema` OpenAPI 어노테이션 포함.
- `RegistrationReq`: `email`, `password`, `passwordChk`, `nick`(`@Pattern` 영문+숫자+언더바 2~20자), `profile`(`@NotBlank`) + `@AssertTrue isPasswordMatch()` 커스텀 교차검증 (`@Schema(hidden=true)`로 Swagger에서 숨김).
- `AuthRes`: `UserWithPostCountRes user`, `String accessToken` — `from(User, accessToken, countPosts)`.

**User**
- `UserRes`: `id`(long), `email`, `nick`, `role`, `profile`, `createdAt` — `from(User)`.
- `UserWithPostCountRes`: `UserRes user`, `long countPosts` — `from(User, countPost)`.

**Post**
- `PostIndexReq`: `page`(`Integer`, `@Min(1)`), `limit`(`Integer`, `@Min(1)`), compact constructor로 기본값 처리.
- `PostStoreReq`(현재 미사용): `content`, `image` — `@Schema`만 있고 검증 어노테이션 없음.
- `PostIndexRes`: `total`(long), `lastPage`(boolean), `posts`(`List<PostWithUserRes>`).
- `PostWithUserRes`: `id`, `content`, `image`, **`createAt`(오타, `createdAt` 아님)**, `updatedAt`, `deletedAt`, `user`(`UserRes`).

**File**
- `FileRes`(`@Builder`): 필드 `fileUri` 하나.

검증은 Jakarta Bean Validation (`@NotBlank`, `@Pattern`, `@Min`, `@AssertTrue`) 사용. 모든 Request DTO에 springdoc `@Schema` 부착(§9).

---

## 7. Security / JWT

- `SecurityConfiguration`: `@Configuration @EnableWebSecurity`. `PasswordEncoder`(`BCryptPasswordEncoder`) 빈, `CorsConfig` 기반 `CorsConfigurationSource` 빈. `SecurityFilterChain`: stateless, httpBasic/formLogin/csrf 비활성, CORS 활성, `TokenAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 삽입, `SecurityUrlRegistry`의 HTTP 메서드별 배열로 인가 규칙 구성, 예외 처리는 `SecurityExceptionHandler`로 위임.
- `SecurityUrlRegistry`: static `String[]` 5개(GET/POST/PUT/PATCH/DELETE) — **인증이 필요한 URL 화이트리스트** 방식. 현재 내용은 `/api/logout`, `/api/posts`(POST, 죽은 엔드포인트), `/api/posts/{id}`(GET, DELETE — DELETE는 미구현).
- `TokenAuthenticationFilter`(`OncePerRequestFilter`): `JwtProvider.extractAccessToken`으로 토큰 추출 → 있으면 `SecurityAuthenticationProvider`로 `Authentication` 생성 후 `SecurityContextHolder`에 등록. 예외는 `HandlerExceptionResolver`로 위임(=`GlobalExceptionHandler`가 처리).
- `SecurityAuthenticationProvider`: JJWT `Claims`를 그대로 `UsernamePasswordAuthenticationToken(claims, null, List.of())`로 감쌈 — 권한(`GrantedAuthority`) 목록은 비어있음(role은 raw JWT claim으로만 조회 가능).
- `SecurityExceptionHandler`: `AuthenticationEntryPoint`(401)+`AccessDeniedHandler`(403) 둘 다 구현, `HandlerExceptionResolver`로 위임해 동일한 `@RestControllerAdvice` 응답 생성.
- `JwtProvider`: JJWT 0.12 fluent API 기반. `generateAccessToken`/`generateRefreshToken`(TTL 상이), subject=userId, issuer/type/claim("role", ...) 설정. `extractRefreshToken`은 쿠키에서, `extractAccessToken`은 `Authorization` 헤더(`scheme` 설정 가능)에서 추출. `extractClaims`가 JJWT 예외들(`ExpiredJwtException` 등)을 앱의 `InvalidTokenException`으로 변환.
- `JwtConfig`(record, `@ConfigurationProperties(prefix="security.jwt")`): `secure`, `issuer`, `type`, `accessTokenExpiry`, `refreshTokenExpiry`, `refreshTokenCookieName`, `refreshTokenCookieExpiry`, `secret`, `headerKey`, `scheme`, `reissueUri`.
- `CookieManager`: `getCookie(request, name)`, `setCookie(response, name, value, maxAge, path)` — `HttpOnly=true`, `Secure=jwtConfig.secure()`.
- Refresh Token 흐름: DB(`User.refreshToken`, 로그아웃 시 null 처리) + HttpOnly 쿠키(reissue URI 경로 한정) 이중 저장 — 구 문서와 전략은 동일, JPA 기반으로 재구현된 것뿐.
- `ProviderPolicy`/`RolePolicy`는 `global/security/constant/` 패키지에 위치.
- 신규 요소: 컨트롤러/DTO 전반에 springdoc `@Operation`/`@Tag`/`@Schema` 사용, 커스텀 `@CustomApiResponse` 메타 어노테이션(§9) — 구 MyBatis 시절에는 없었음.

---

## 8. 전역/공통 코드

**`GlobalRes<T>`** (record): `code`, `message`, `data`. 정적 팩토리: `from(CustomResponseCode, T)`, `from(CustomResponseCode)`, `success(T)`, `success()`.

> **주의**: `message`는 **enum 상수명 그대로**(`"SUCCESS"`, `"NOT_REGISTERED_ERROR"` 등) 세팅된다. 구 문서 예시(`"message": "정상 처리"`)와 다르다. 실제 응답은 `{"code":"00","message":"SUCCESS","data":{...}}` 형태다.

**`CustomResponseCode`** enum:

| Enum | HTTP | code |
|---|---|---|
| SUCCESS | 200 | 00 |
| NOT_REGISTERED_ERROR | 401 | E01 |
| UNAUTHENTICATED_ERROR | 401 | E02 |
| UNAUTHORIZED_ERROR | 403 | E03 |
| INVALID_TOKEN_ERROR | 401 | E04 |
| NOT_FOUND_DATA_ERROR | 404 | E10 |
| DUPLICATED_DATA_ERROR | 409 | E11 |
| INVALID_PARAMETER_ERROR | 400 | E21 |
| FILE_MANAGED_ERROR | 500 | E40 |
| DB_ERROR | 500 | E80 |
| SYSTEM_ERROR | 500 | E99 |

> 구 문서의 `E20`(404 unknown URL), `E30`(400 file/runtime)은 더 이상 없음. 대신 `E10`/`E11`/`E40`/`E02`/`E03`이 새로 추가/분리됨.

**`GlobalExceptionHandler`** (`@RestControllerAdvice`) — 처리 예외 목록:
`NotRegisteredException`→NOT_REGISTERED_ERROR, `AuthenticationException`→UNAUTHENTICATED_ERROR, `AccessDeniedException`→UNAUTHORIZED_ERROR, `InvalidTokenException`→INVALID_TOKEN_ERROR, `DeletedRecordException`→NOT_FOUND_DATA_ERROR, `DuplicatedRecordException`→DUPLICATED_DATA_ERROR, `MethodArgumentTypeMismatchException`→INVALID_PARAMETER_ERROR, `MethodArgumentNotValidException`→INVALID_PARAMETER_ERROR(필드별 오류는 로깅만, 응답 data는 null), `FileManagedException`→FILE_MANAGED_ERROR, `SQLException`→DB_ERROR, `Exception`(catch-all)→SYSTEM_ERROR.

**커스텀 예외** (`global/errors/custom/`): `NotRegisteredException`, `InvalidTokenException`, `DeletedRecordException`, `FileManagedException`, `DuplicatedRecordException`, 그리고 **미사용 dead code**인 `DuplicatedUserException`(핸들러도 없음, 문서화 제외 대상).

**파일 업로드**:
- `FileConfig`(`@ConfigurationProperties(prefix="file")`): `serverUri`, `storagePath`, `profilePath`, `postPath`, `allowExtensionList`(예: `image/jpg`, `image/jpeg`, `image/png`, `image/gif`, `image/svg`, `image/webp`).
- `LocalFileManager`: `extractExtension(file)`(확장자 검증, 실패 시 `FileManagedException`), `generateFileName()`(`yyyyMMdd_UUID`), `generateProfilePath`/`generatePostPath`, `makeDir`, `saveFile`(물리 경로 결정 + 저장, IO 실패 시 `FileManagedException`).
- `WebConfig`: `/files/**` → `{storagePath}/files` 정적 리소스 매핑.

**CORS**: `CorsConfig`(`@ConfigurationProperties(prefix="cors")`): `allowedOrigins`, `maxAge` — `SecurityConfiguration.corsConfigurationSource()`에서 사용.

**QueryDSL 설정**: `QueryDSLConfig` — `EntityManager`로부터 `JPAQueryFactory` 빈 생성.

---

## 9. OpenAPI / Swagger

`springdoc-openapi-starter-webmvc-ui:2.8.16` 실사용 중.

- `OpenApiConfig`: 루트 `OpenAPI` 빈 (`Info`: title "Meerkatgram API", description "Meerkatgram REST API Document", version "v1.0.0").
- `application.yaml` `springdoc:` 블록: 기본 consumes/produces `application/json`, `swagger-ui.path: /swagger-ui.html`, `operations-sorter: alpha`, `api-docs.path: /api-docs`.
- 컨트롤러별 `@Tag`(인증 API / 게시글 API / 파일 API — UserController는 빈 컨트롤러라 없음), 메서드별 `@Operation`.
- Request DTO(`LoginReq`, `RegistrationReq`, `PostIndexReq`, `PostStoreReq`)에 필드별 `@Schema(description, example, nullable, requiredMode)`. `RegistrationReq.isPasswordMatch()`는 `@Schema(hidden=true)`로 스키마에서 숨김.
- 커스텀 메타 어노테이션 `CustomApiResponse`(`@Target(METHOD)`): 기본 `@ApiResponse(200, "SUCCESS")` + `CustomResponseCode[] value()`로 각 메서드가 반환 가능한 에러코드 선언 가능.
- `ApiResponseCustomizer`(springdoc `OperationCustomizer` 구현): 문서 생성 시 `@CustomApiResponse`를 읽어 HTTP 상태별로 그룹핑, 예시 JSON(`{"code":...,"message":...,"data":null}`)을 자동 생성해 Swagger UI 응답 예시에 반영.

이 OpenAPI 에러 예시 자동생성 메커니즘(`CustomApiResponse` + `ApiResponseCustomizer`)은 완전히 신규이며 구 문서에 없다.

---

## 10. application.yaml 구조

`src/main/resources/`에 **`application.yaml`**(dev/default), **`application-prod.yaml`**(prod)이 존재하며 **git에 그대로 커밋**되어 있다(gitignore 대상 아님, 개발용 기본 시크릿 포함). 별도 `application-example.yaml`/`.env.example` 템플릿은 없음.

`application.yaml` 주요 섹션:
- `spring.application.name`/`version`
- `spring.servlet.multipart`: `max-file-size: 10MB`, `max-request-size: 20MB`
- `spring.datasource`: MySQL URL이 `${DB_HOST:localhost}`/`${DB_PORT:3306}`/`${DB_NAME:meerkatgram}`/`${DB_USER:root}`/`${DB_PASSWORD:msa505}`로 구성, HikariCP(`maximum-pool-size:10`, `connection-timeout:30000`)
- `spring.jpa`: `hibernate.ddl-auto: none`(주석으로 위험성 설명), `show-sql: true`, `format_sql: true`, `default_batch_fetch_size: 100`, `defer-datasource-initialization: true`
- `spring.sql.init`: `data-locations: classpath*:dummy/*.sql`, `mode: never`(기본 비활성), `platform: all`
- `server.error`: dev는 `include-stacktrace/include-message: always`, prod는 `never`
- `springdoc:` 블록 (§9)
- `logging.file.name: logs/error.log`, `logging.level.root: info` + `com.msa4meerkatgram.global.errors` 디버그 오버라이드 (단, 존재하지 않는 `com.msa4spring.controllers` 패키지 참조가 남아있음 — 이전 템플릿 잔재)
- `security.jwt:` 블록은 `JwtConfig` 필드와 1:1 대응, `reissue-uri: /api/reissue-token`
- `file:` 블록은 `FileConfig` 필드와 1:1 대응
- `cors:` 블록: `allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}`, `max-age: 3600`

`application-prod.yaml` 차이점: DB 환경변수 기본값 없음(`${DB_HOST}` 등 필수 지정), `server.error.*: never`, 로그레벨 `root: error`, 그리고 **잔재/dead**인 `mybatis:` 블록(`mapper-locations`, `type-aliases-package`) — MyBatis 의존성이 없는데도 남아있음.

`src/main/resources/dummy/`: `01_dummy_users.sql`, `02_dummy_posts.sql` (HeidiSQL export, `DELETE`+`INSERT`) — `spring.sql.init.mode`를 바꿔야 활성화됨.

---

## 11. 데이터베이스 스키마 (JPA 엔티티 + dummy SQL로부터 추정, schema.sql 없음)

`ddl-auto: none`, Flyway/Liquibase 없음 — 스키마는 엔티티 + dummy insert 데이터로부터 추정.

**users**

| 컬럼 | 타입(추정) | NULL | 비고 |
|---|---|---|---|
| id | BIGINT UNSIGNED | NO | PK, AUTO_INCREMENT |
| email | VARCHAR(100) | NO | UNIQUE |
| password | VARCHAR(255) | NO | BCrypt |
| nick | VARCHAR(20) | NO | **unique 아님** (구 문서와 다름) |
| provider | VARCHAR(10) | NO | NONE/KAKAO/GOOGLE |
| role | VARCHAR(10) | NO | NORMAL/SUPER |
| profile | VARCHAR(100) | NO | |
| refresh_token | VARCHAR(255) | YES | 로그아웃 시 null |
| created_at | DATETIME | NO | 자동(`@CreatedDate`) |
| updated_at | DATETIME | NO | 자동(`@LastModifiedDate`) |
| deleted_at | DATETIME | YES | 소프트 삭제 |

**posts**

| 컬럼 | 타입(추정) | NULL | 비고 |
|---|---|---|---|
| id | BIGINT UNSIGNED | NO | PK, AUTO_INCREMENT |
| user_id | BIGINT UNSIGNED | NO | FK 성격이나 **물리적 FK 제약 없음**(`NO_CONSTRAINT`), 수정 불가 |
| content | VARCHAR(200) | NO | |
| image | VARCHAR(100) | NO | 엔티티상 not-null |
| created_at | DATETIME | NO | 자동 |
| updated_at | DATETIME | NO | 자동 |
| deleted_at | DATETIME | YES | 소프트 삭제 |

소프트 삭제: Hibernate `@SQLDelete`(물리 DELETE 대신 UPDATE 발동) + `@SQLRestriction("deleted_at IS NULL")`(모든 SELECT에 자동 적용, QueryDSL 포함) — MyBatis 시절 "매 쿼리에 수동으로 `WHERE deleted_at IS NULL` 추가" 패턴을 대체. 구 문서의 `ResultMap`/`useGeneratedKeys` 섹션 전체가 이제 해당 없음.

**`comments`/`likes`/`notifications`/`push_subscriptions` 테이블/엔티티는 존재하지 않는다.** 구 문서의 "v2 예정" 라벨 그대로이므로, 신규 문서에서는 삭제하거나 "계획 중(미구현)"으로 명확히 구분해야 한다.

---

## 12. `doc/1st-doc/` 대비 핵심 불일치 목록 (문서 수정 시 반영할 것)

1. **영속성 계층**: MyBatis → Spring Data JPA + QueryDSL (소프트 삭제는 `@SQLDelete`/`@SQLRestriction`). Mapper XML 완전 제거.
2. **도메인 범위**: 여전히 `user`/`post`/`auth`/`file`뿐 — `comment`/`like`/`notification`/`push_subscription`은 코드에 없음(구 ERD의 "v2 예정" 그대로).
3. **엔드포인트 변경/누락**:
   - 회원가입은 `POST /api/registration`(AuthController), `POST /api/users` 아님.
   - `GET /api/users/{id}` 없음 — `UserController`는 빈 스텁.
   - 파일 업로드 경로는 `/api/files/profiles`/`/api/files/posts` (구: `/api/images/...`), 응답 필드는 `fileUri`(구: `fileUrl`).
   - `POST /api/posts`(작성), `DELETE /api/posts/{id}`(삭제)는 주석처리/미구현 상태. `SecurityUrlRegistry`에는 여전히 인증 필요로 등록돼 있음.
4. **응답 포맷**: `message`는 enum 상수명 그대로(`"SUCCESS"` 등), 한글 문장 아님.
5. **에러 코드 재구성**: 구 `E20`/`E30` 삭제, 신규 `E02`/`E03`/`E10`/`E11`/`E40` 추가.
6. **OpenAPI/Swagger 신규 도입**: springdoc 2.8.16, `@Tag`/`@Operation`/`@Schema` 전면 적용 + 커스텀 `@CustomApiResponse`/`ApiResponseCustomizer`로 Swagger 에러 예시 자동생성 (`/swagger-ui.html`, `/api-docs`).
7. **엔티티 필드 차이**: `nick`은 `VARCHAR(20)`이며 unique 아님(구: `VARCHAR(15)` UK); `role`은 `NORMAL`로 정정됨(구 오타 `NOMAL`); `posts.image`는 not-null.
8. **Dead/잔재 코드(문서화 제외 대상)**: `DuplicatedUserException`(미사용), `application-prod.yaml`의 잔재 `mybatis:` 블록, `application.yaml`의 존재하지 않는 `com.msa4spring.controllers` 로거 참조, 빈 `domain/auth/entities` 디렉토리.
9. **신규 인프라**: `Dockerfile`(멀티스테이지), `Jenkinsfile`(Docker 빌드/푸시 + GitOps 배포) — docker-compose는 없음.
