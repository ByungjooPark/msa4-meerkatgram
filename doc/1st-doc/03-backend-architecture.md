# 03. 백엔드 아키텍처

## 1. 레이어드 아키텍처 개념

이 프로젝트는 **레이어드 아키텍처(Layered Architecture)** 를 따른다.
각 레이어는 자신의 역할에만 집중하고, 반드시 인접한 레이어와만 통신한다.

```
┌─────────────────────────────────────┐
│         Client (Vue 3 / Postman)    │
└────────────────┬────────────────────┘
                 │ HTTP 요청
┌────────────────▼────────────────────┐
│      Filter Layer (Spring Security) │  JWT 검증 (인증), SecurityContext 등록
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│       Controller Layer              │  @PreAuthorize 인가 체크, 요청 수신, 입력값 검증, 응답 반환
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│         Service Layer               │  비즈니스 로직 처리
└────────────────┬────────────────────┘
                 │
┌────────────────▼──────────────────────────────────┐
│  Repository Layer (Spring Data JPA + QueryDSL)     │  엔티티 CRUD, 동적/복잡 조회
└────────────────┬──────────────────────────────────┘
                 │
┌────────────────▼────────────────────┐
│         MySQL Database              │
└─────────────────────────────────────┘
```

| 레이어 | 책임 | 금지 사항 |
|--------|------|-----------|
| Filter | JWT 토큰 검증(인증), SecurityContext 등록 | 인가(역할) 판단, 비즈니스 로직 처리 |
| Controller | `@PreAuthorize` 인가 체크, HTTP 요청 수신·응답 반환, `@Valid` 검증 | SQL 직접 실행, 비즈니스 로직 |
| Service | 비즈니스 로직, 트랜잭션 관리 | HTTP 관련 코드 (`HttpServletRequest` 등) |
| Repository | 엔티티 CRUD(Spring Data JPA), 동적 조회(QueryDSL) | 비즈니스 로직 |

---

## 2. 패키지 구조

```
src/main/java/com/msa4meerkatgram/
│
├── Msa4MeerkatgramApplication.java      ← 진입점 (@SpringBootApplication)
│
├── domain/                              ← 비즈니스 도메인별 코드
│   ├── auth/                            ← 인증 (로그인, 로그아웃, 토큰 재발급, 회원가입)
│   │   ├── controllers/
│   │   │   └── AuthController.java
│   │   ├── entities/                    ← (비어있음) auth 전용 엔티티 없이 User를 재사용
│   │   ├── repositories/
│   │   │   └── AuthRepository.java      ← JpaRepository<User, Long>
│   │   ├── requests/
│   │   │   ├── LoginReq.java
│   │   │   └── RegistrationReq.java
│   │   ├── responses/
│   │   │   └── AuthRes.java
│   │   └── services/
│   │       └── AuthService.java
│   │
│   ├── file/                            ← 파일 업로드 (DB 테이블 없는 무상태 도메인)
│   │   ├── controllers/
│   │   │   └── FileController.java
│   │   ├── responses/
│   │   │   └── FileRes.java
│   │   └── services/
│   │       └── FileService.java
│   │
│   ├── post/                            ← 게시글 CRUD, 페이지네이션
│   │   ├── controllers/
│   │   ├── entities/
│   │   ├── repositories/                ← PostRepository(JPA), PostQueryRepository(QueryDSL)
│   │   ├── requests/
│   │   ├── responses/                   ← PostWithoutUserRes.java는 미사용(dead code)
│   │   └── services/
│   │
│   └── user/                            ← 현재는 빈 스텁(공개 엔드포인트 없음)
│       ├── controllers/                 ← UserController: 엔드포인트 0개
│       ├── entities/
│       ├── repositories/                ← UserRepository (커스텀 메서드 없음, 미사용)
│       ├── responses/
│       └── services/                    ← UserService: 빈 스텁
│
└── global/                              ← 전 도메인 공통 코드
    ├── config/
    │   ├── CorsConfig.java              ← CORS 설정값 (@ConfigurationProperties)
    │   ├── WebConfig.java               ← 정적 리소스(/files/**) 경로 설정
    │   ├── jpa/
    │   │   └── QueryDSLConfig.java      ← JPAQueryFactory 빈 등록
    │   └── openapi/
    │       ├── OpenApiConfig.java       ← springdoc 루트 OpenAPI 빈
    │       ├── CustomApiResponse.java   ← 에러코드 선언용 메타 어노테이션
    │       └── ApiResponseCustomizer.java ← Swagger 에러 응답 예시 자동생성
    ├── errors/
    │   ├── GlobalExceptionHandler.java  ← @RestControllerAdvice 에러 핸들러
    │   └── custom/
    │       ├── NotRegisteredException.java
    │       ├── InvalidTokenException.java
    │       ├── DeletedRecordException.java
    │       ├── DuplicatedRecordException.java
    │       ├── FileManagedException.java
    │       └── DuplicatedUserException.java   ← 미사용(dead code, 핸들러도 없음)
    ├── responses/
    │   └── GlobalRes.java               ← 공통 응답 DTO (record)
    ├── security/
    │   ├── constant/                    ← ProviderPolicy, RolePolicy (Enum)
    │   ├── cookie/
    │   │   └── CookieManager.java       ← Refresh Token 쿠키 생성/삭제
    │   ├── filter/
    │   │   ├── SecurityConfiguration.java     ← FilterChain 설정, @EnableMethodSecurity로 @PreAuthorize 활성화
    │   │   ├── SecurityAuthenticationProvider.java ← Claims의 role을 SimpleGrantedAuthority로 변환
    │   │   └── TokenAuthenticationFilter.java ← JWT 검증 필터
    │   └── jwt/
    │       ├── JwtConfig.java           ← JWT 설정값 (@ConfigurationProperties)
    │       └── JwtProvider.java         ← 토큰 생성·검증·파싱
    └── util/
        └── file/
            ├── FileConfig.java          ← 파일 저장 경로 설정
            └── LocalFileManager.java    ← 실제 파일 저장 로직

src/main/resources/
├── application.yaml                     ← 환경 설정 (dev/default, git에 커밋됨)
├── application-prod.yaml                ← 운영 환경 설정
└── dummy/                               ← 개발용 더미 INSERT SQL (기본 비활성)
```

---

## 3. `domain` vs `global` 분리 이유

| 구분 | `domain/` | `global/` |
|------|-----------|-----------|
| 성격 | 비즈니스 기능 단위 | 모든 도메인에 공통으로 사용되는 코드 |
| 예시 | 게시글 작성, 회원가입, 로그인 | JWT 처리, 에러 핸들링, CORS 설정 |
| 변경 이유 | 비즈니스 요구사항 변경 | 기술적 설정 변경 |
| 의존 방향 | `domain` → `global` (단방향) | `global`은 `domain`을 모른다 |

> `domain`이 `global`에 의존하는 건 허용하지만, 반대 방향은 금지한다.
> 예를 들어 `GlobalExceptionHandler`는 어떤 도메인의 예외든 처리할 수 있어야 하므로 `global`에 위치한다.

---

## 4. 요청 처리 흐름 — 회원가입 예시

`POST /api/registration` 요청이 들어왔을 때 코드가 실행되는 순서를 따라간다.

### Step 1. Controller — 요청 수신 및 검증

```java
// AuthController.java
@PostMapping("/registration")
public ResponseEntity<GlobalRes<Void>> registration(
    @Valid @RequestBody RegistrationReq registrationReq  // ① @Valid로 입력값 검증
) {
    authService.registration(registrationReq);           // ② Service 호출

    return ResponseEntity.ok(GlobalRes.success());        // ③ 공통 응답 포장 (data 없음)
}
```

- `@Valid`: `RegistrationReq`의 `@NotBlank`/`@Pattern`/`@AssertTrue` 검증 어노테이션을 실행한다. 실패하면 `MethodArgumentNotValidException` 발생 → `GlobalExceptionHandler`가 처리
- Controller는 비즈니스 로직을 직접 처리하지 않고, Service에 위임만 한다
- 회원가입은 응답으로 별도 데이터를 돌려주지 않는다(`GlobalRes<Void>`)

### Step 2. Request DTO — 입력값 정의

```java
// RegistrationReq.java
public record RegistrationReq(
    @NotBlank(message = "이메일은 필수 항목입니다.")
    @Pattern(regexp = "...", message = "허용하지 않는 이메일 양식입니다.")
    String email,

    @NotBlank(message = "비밀번호는 필수 항목입니다.")
    @Pattern(regexp = "^[0-9a-zA-Z!@#$%^&*()]{8,20}$", message = "허용하지 않는 비밀번호 양식입니다.")
    String password,

    @NotBlank(message = "비밀번호 확인은 필수 항목입니다.")
    String passwordChk,

    @NotBlank(message = "닉네임은 필수 항목입니다.")
    @Pattern(regexp = "^[0-9a-zA-Z_]{2,20}$", message = "허용하지 않는 닉네임 양식입니다.")
    String nick,

    @NotBlank(message = "프로필은 필수 항목입니다.")
    String profile
) {
    @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordMatch() {
        return password != null && password.equals(passwordChk);
    }
}
```

- Java `record`를 사용해 불변(immutable) DTO를 정의한다
- getter, equals, hashCode, toString이 자동 생성된다
- `isPasswordMatch()`처럼 `@AssertTrue`가 붙은 메서드는 `@Valid` 검증 시 함께 실행되는 커스텀 교차검증이다(비밀번호/비밀번호 확인 일치 여부)
- 요청에서 받는 데이터만 포함하고, Entity(`User`)와는 별개 클래스로 분리한다

### Step 3. Service — 비즈니스 로직

```java
// AuthService.java
@Transactional(rollbackFor = Exception.class)
public void registration(RegistrationReq registrationReq) {
    // ① 중복 이메일 확인 (exists 쿼리 — 대용량 환경에서 findByEmail보다 효율적)
    if (authRepository.existsByEmail(registrationReq.email())) {
        throw new DuplicatedRecordException("이미 가입된 회원입니다.");
    }

    // ② Entity 생성 및 값 세팅
    User newUser = new User();
    newUser.setEmail(registrationReq.email());
    newUser.setPassword(passwordEncoder.encode(registrationReq.password())); // BCrypt 암호화
    newUser.setNick(registrationReq.nick());
    newUser.setProfile(registrationReq.profile());
    newUser.setProvider(ProviderPolicy.NONE);
    newUser.setRole(RolePolicy.NORMAL);

    // ③ DB 저장
    authRepository.save(newUser);
}
```

> 회원가입은 응답으로 유저 정보를 돌려주지 않으므로(Void), Entity → Response DTO 변환 과정이 없다.
> 참고로 로그인(`AuthService.login`)의 경우 `AuthRes.from(user, accessToken, countPosts)`처럼
> Entity의 `password`, `refreshToken` 같은 민감 필드를 제외한 Response DTO로 변환해서 반환한다.

### Step 4. Repository — DB 접근 (Spring Data JPA)

```java
// AuthRepository.java (인터페이스)
public interface AuthRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

- `JpaRepository<User, Long>`을 상속하면 `save()`, `findById()`, `count()` 등 기본 CRUD 메서드를 별도 구현 없이 바로 사용할 수 있다
- `existsByEmail`처럼 메서드 이름 규칙(`exists + By + 필드명`)만 지키면, Spring Data JPA가 이름을 분석해 쿼리를 자동 생성한다(쿼리 메서드)
- SQL을 직접 작성하지 않아도 되며, INSERT 시 PK 자동 채번은 `User` 엔티티의 `@GeneratedValue(strategy = GenerationType.IDENTITY)`가 처리한다

---

## 5. 공통 응답 구조 (`GlobalRes<T>`)

모든 API 응답은 아래 클래스로 포장해서 반환한다.

```java
// GlobalRes.java — @Builder 클래스가 아니라 record다
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

- `record`이므로 `.builder()...build()`가 아니라 정적 팩토리 메서드(`from`, `success`)로 생성한다
- `message`에는 한글 문장이 아니라 `CustomResponseCode` enum 상수명이 그대로 들어간다(`customResponseCode.name()`)

**Controller에서의 사용 패턴**
```java
// 데이터 있는 성공 응답
return ResponseEntity.ok(GlobalRes.success(result));

// 데이터 없는 성공 응답 (회원가입, 로그아웃 등)
return ResponseEntity.ok(GlobalRes.success());
```

**성공 응답 예시**
```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "nick": "meerkat"
  }
}
```

**에러 응답 예시** (`GlobalExceptionHandler`가 반환)
```json
{
  "code": "E01",
  "message": "NOT_REGISTERED_ERROR",
  "data": null
}
```

---

## 6. 에러 처리 구조 (`@RestControllerAdvice`)

```java
// GlobalExceptionHandler.java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<GlobalRes<Void>> generateErrorResponse(CustomResponseCode customResponseCode) {
        return ResponseEntity.status(customResponseCode.getHttpStatus())
            .body(GlobalRes.<Void>from(customResponseCode));
    }

    @ExceptionHandler(NotRegisteredException.class)
    public ResponseEntity<GlobalRes<Void>> notRegisteredHandle(NotRegisteredException e) {
        log.debug(CustomResponseCode.NOT_REGISTERED_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.NOT_REGISTERED_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalRes<Void>> methodArgumentNotValidHandle(MethodArgumentNotValidException e) {
        // 필드별 오류 메시지는 로깅만 하고, 응답 data는 null로 통일한다
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, fe -> fe.getDefaultMessage(), (a, b) -> a));
        log.debug(CustomResponseCode.INVALID_PARAMETER_ERROR.name(), errors);
        return this.generateErrorResponse(CustomResponseCode.INVALID_PARAMETER_ERROR);
    }
    // ... 이하 생략
}
```

- 모든 핸들러가 `data`에 별도 메시지를 담지 않고 공통 `generateErrorResponse()`를 통해 `GlobalRes<Void>`(즉 `data: null`)를 반환한다
- 예외 상세 메시지는 응답이 아니라 로그(`log.debug`/`log.error`)로만 남긴다 — 클라이언트에 내부 정보를 노출하지 않기 위함

| 예외 클래스 | 구분 | HTTP | 코드 | 발생 상황 |
|------------|------|------|------|-----------|
| `NotRegisteredException` | 커스텀 | 401 | E01 | 이메일/비밀번호 불일치 |
| `AccessDeniedException` | Spring Security | 401 또는 403 | E02 또는 E03 | `@PreAuthorize` 인가 실패. 익명 사용자(미로그인)면 E02(401), 로그인은 했으나 역할 부족이면 E03(403) — `SecurityContext`의 인증 객체를 보고 핸들러가 직접 분기 |
| `InvalidTokenException` | 커스텀 | 401 | E04 | 토큰 형식/서명 오류 |
| `DeletedRecordException` | 커스텀 | 404 | E10 | 조회 대상이 이미 삭제됨/존재하지 않음 |
| `DuplicatedRecordException` | 커스텀 | 409 | E11 | 이메일 등 중복 데이터 |
| `MethodArgumentTypeMismatchException` | Spring MVC | 400 | E21 | 경로 변수 타입 오류 (`/posts/abc`) |
| `MethodArgumentNotValidException` | Spring MVC | 400 | E21 | `@Valid` 검증 실패 |
| `FileManagedException` | 커스텀 | 500 | E40 | 파일 저장/삭제 실패 |
| `NoResourceFoundException` | Spring MVC | 404 | E50 | 존재하지 않는 URL 요청 |
| `SQLException` | Java 표준 | 500 | E80 | DB 에러 |
| `Exception` | Java 표준 | 500 | E99 | 알 수 없는 시스템 에러 |

> 구 버전에 있던 `E20`(존재하지 않는 URL), `E30`(파일/런타임 에러 통합)은 더 이상 사용하지 않는다.
> 존재하지 않는 URL 처리는 `E50`(`NOT_FOUND_ERROR`)으로 새로 추가되었다.

> `@RestControllerAdvice`는 모든 `@RestController`에서 발생하는 예외를 한 곳에서 처리한다.
> 각 Controller에 try-catch를 반복하지 않아도 되므로 코드가 간결해진다.
