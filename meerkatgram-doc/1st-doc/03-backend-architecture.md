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
│      Filter Layer (Spring Security) │  JWT 검증, 인증/인가
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│       Controller Layer              │  요청 수신, 입력값 검증, 응답 반환
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│         Service Layer               │  비즈니스 로직 처리
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│         Mapper Layer (MyBatis)      │  SQL 실행, 결과 매핑
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│         MySQL Database              │
└─────────────────────────────────────┘
```

| 레이어 | 책임 | 금지 사항 |
|--------|------|-----------|
| Filter | JWT 토큰 검증, SecurityContext 등록 | 비즈니스 로직 처리 |
| Controller | HTTP 요청 수신·응답 반환, `@Valid` 검증 | SQL 직접 실행, 비즈니스 로직 |
| Service | 비즈니스 로직, 트랜잭션 관리 | HTTP 관련 코드 (`HttpServletRequest` 등) |
| Mapper | SQL 실행, DB 결과 → Java 객체 변환 | 비즈니스 로직 |

---

## 2. 패키지 구조

```
src/main/java/com/msa4meerkatgram/
│
├── Msa4MeerkatgramApplication.java      ← 진입점 (@SpringBootApplication)
│
├── domain/                              ← 비즈니스 도메인별 코드
│   ├── auth/                            ← 인증 (로그인, 로그아웃, 토큰 재발급)
│   │   ├── controllers/
│   │   │   └── AuthController.java
│   │   ├── mapper/
│   │   │   └── AuthMapper.java
│   │   ├── requests/
│   │   │   └── LoginReq.java
│   │   ├── responses/
│   │   │   └── AuthRes.java
│   │   └── services/
│   │       └── AuthService.java
│   │
│   ├── file/                            ← 파일 업로드
│   │   ├── controleers/
│   │   │   └── FileController.java
│   │   ├── responses/
│   │   │   └── FileRes.java
│   │   └── services/
│   │       └── FileService.java
│   │
│   ├── post/                            ← 게시글 CRUD, 페이지네이션
│   │   ├── controllers/
│   │   ├── entities/
│   │   ├── mapper/
│   │   ├── requests/
│   │   ├── responses/
│   │   └── services/
│   │
│   └── user/                            ← 회원가입, 유저 조회
│       ├── constant/                    ← Enum (RolePolicy, ProviderPolicy)
│       ├── controllers/
│       ├── entities/
│       ├── mapper/
│       ├── requests/
│       ├── responses/
│       └── services/
│
└── global/                              ← 전 도메인 공통 코드
    ├── config/
    │   ├── CorsConfig.java              ← CORS 설정값 (@ConfigurationProperties)
    │   └── WebConfig.java               ← 정적 리소스(이미지) 경로 설정
    ├── errors/
    │   ├── GlobalExceptionHandler.java  ← @RestControllerAdvice 에러 핸들러
    │   └── custom/
    │       ├── FileStorageException.java
    │       ├── InvalidTokenException.java
    │       └── NotRegisteredException.java
    ├── responses/
    │   └── GlobalRes.java               ← 공통 응답 DTO
    ├── security/
    │   ├── cookie/
    │   │   └── CookieManager.java       ← Refresh Token 쿠키 생성/삭제
    │   ├── filter/
    │   │   ├── SecurityConfiguration.java     ← FilterChain 설정
    │   │   ├── SecurityAuthenticationProvider.java
    │   │   ├── SecurityExceptionHandler.java  ← 인증/인가 실패 처리
    │   │   ├── SecurityUrlRegistry.java       ← 공개/인증필요 URL 목록
    │   │   └── TokenAuthenticationFilter.java ← JWT 검증 필터
    │   └── jwt/
    │       ├── JwtConfig.java           ← JWT 설정값 (@ConfigurationProperties)
    │       └── JwtTokenProvider.java    ← 토큰 생성·검증·파싱
    └── util/
        └── file/
            ├── FileConfig.java          ← 파일 저장 경로 설정
            └── LocalFileManager.java    ← 실제 파일 저장 로직

src/main/resources/
├── mapper/
│   ├── auth/AuthMapper.xml
│   ├── posts/PostMapper.xml
│   └── user/UserMapper.xml
└── application.yaml                     ← 환경 설정 (git 제외)
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

`POST /api/users` 요청이 들어왔을 때 코드가 실행되는 순서를 따라간다.

### Step 1. Controller — 요청 수신 및 검증

```java
// UserController.java
@PostMapping("/users")
public ResponseEntity<GlobalRes<UserRes>> store(
    @Valid @RequestBody RegistrationReq registrationRequestDTO  // ① @Valid로 입력값 검증
) {
    UserRes result = userService.store(registrationRequestDTO); // ② Service 호출

    return ResponseEntity.status(200).body(
        GlobalRes.<UserRes>builder()
            .code("00")
            .message("정상 처리")
            .data(result)
            .build()                                            // ③ 공통 응답 포장
    );
}
```

- `@Valid`: `RegistrationReq`의 `@NotBlank` 등 검증 어노테이션을 실행한다. 실패하면 `MethodArgumentNotValidException` 발생 → `GlobalExceptionHandler`가 처리
- Controller는 비즈니스 로직을 직접 처리하지 않고, Service에 위임만 한다

### Step 2. Request DTO — 입력값 정의

```java
// RegistrationReq.java
public record RegistrationReq(
    @NotBlank(message = "필수항목입니다.") String email,
    @NotBlank(message = "필수항목입니다.") String password,
    @NotBlank(message = "필수항목입니다.") String nick,
    @NotNull(message = "필수항목입니다.")  String profile
) {}
```

- Java `record`를 사용해 불변(immutable) DTO를 정의한다
- getter, equals, hashCode, toString이 자동 생성된다
- 요청에서 받는 데이터만 포함하고, Entity(`User`)와는 별개 클래스로 분리한다

### Step 3. Service — 비즈니스 로직

```java
// UserService.java
@Transactional
public UserRes store(RegistrationReq req) {
    // ① 중복 이메일 확인
    User chkUser = userMapper.findByEmail(req.email());
    if (chkUser != null) {
        throw new RuntimeException("이미 가입된 회원입니다.");
    }

    // ② Entity 생성 및 값 세팅
    User user = new User();
    user.setEmail(req.email());
    user.setPassword(passwordEncoder.encode(req.password())); // BCrypt 암호화
    user.setNick(req.nick());
    user.setProfile(req.profile());
    user.setProvider(ProviderPolicy.NONE.getProvider());
    user.setRole(RolePolicy.NORMAL.getRole());

    // ③ DB 저장
    userMapper.create(user);

    // ④ 응답 DTO로 변환 (Entity를 그대로 반환하지 않는다)
    return UserRes.builder()
        .id(user.getId())
        .email(user.getEmail())
        ...
        .build();
}
```

> Entity(`User`)를 Controller에 직접 반환하지 않는 이유:
> Entity에는 `password`, `refreshToken` 같이 클라이언트에 노출하면 안 되는 필드가 있다.
> Response DTO(`UserRes`)로 변환해서 필요한 필드만 반환한다.

### Step 4. Mapper — SQL 실행

```java
// UserMapper.java (인터페이스)
@Mapper
public interface UserMapper {
    User findByEmail(String email);
    int create(User user);
}
```

```xml
<!-- UserMapper.xml -->
<insert id="create" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO users (email, password, nick, provider, role, profile, created_at, updated_at)
    VALUES (#{email}, #{password}, #{nick}, #{provider}, #{role}, #{profile}, NOW(), NOW())
</insert>
```

- `@Mapper`: MyBatis가 이 인터페이스의 구현체를 자동 생성한다
- SQL은 Java 코드가 아닌 XML에 분리해서 작성한다
- `useGeneratedKeys="true"`: INSERT 후 DB가 생성한 PK를 `user.id`에 자동 주입

---

## 5. 공통 응답 구조 (`GlobalRes<T>`)

모든 API 응답은 아래 클래스로 포장해서 반환한다.

```java
// GlobalRes.java
@Getter
@Builder
public class GlobalRes<T> {
    private String code;    // 처리 결과 코드
    private String message; // 처리 결과 메시지
    private T data;         // 응답 데이터 (제네릭)
}
```

**성공 응답 예시**
```json
{
  "code": "00",
  "message": "정상 처리",
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
  "message": "로그인 에러",
  "data": "이메일 또는 비밀번호를 확인해 주세요."
}
```

---

## 6. 에러 처리 구조 (`@RestControllerAdvice`)

```java
// GlobalExceptionHandler.java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotRegisteredException.class)
    public ResponseEntity<GlobalRes<String>> authenticationHandle(NotRegisteredException e) {
        return ResponseEntity.status(401).body(
            GlobalRes.<String>builder().code("E01").message("로그인 에러").data(e.getMessage()).build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalRes<List<String>>> methodArgumentNotValidHandle(MethodArgumentNotValidException e) {
        return ResponseEntity.status(400).body(
            GlobalRes.<List<String>>builder()
                .code("E21")
                .message("요청 파라미터에 이상이 있습니다.")
                .data(e.getBindingResult().getAllErrors().stream()
                    .map(item -> String.format("%s : 잘못된 값입니다.", item.getObjectName()))
                    .toList())
                .build()
        );
    }
    // ... 이하 생략
}
```

| 예외 클래스 | 구분 | HTTP | 코드 | 발생 상황 |
|------------|------|------|------|-----------|
| `NotRegisteredException` | 커스텀 | 401 | E01 | 이메일/비밀번호 불일치 |
| `AuthenticationException` | Spring Security | 401 | E02 | 인증 토큰 없음 |
| `AccessDeniedException` | Spring Security | 403 | E03 | 권한 부족 |
| `InvalidTokenException` | 커스텀 | 400 | E04 | 토큰 형식/서명 오류 |
| `NoResourceFoundException` | Spring MVC | 404 | E20 | 존재하지 않는 URL |
| `MethodArgumentTypeMismatchException` | Spring MVC | 400 | E21 | 경로 변수 타입 오류 (`/posts/abc`) |
| `MethodArgumentNotValidException` | Spring MVC | 400 | E21 | `@Valid` 검증 실패 |
| `FileStorageException` | 커스텀 | 400 | E30 | 파일 저장 실패 |
| `RuntimeException` | Java 표준 | 400 | E30 | 기타 런타임 에러 |
| `SQLException` | Java 표준 | 500 | E80 | DB 에러 |
| `Exception` | Java 표준 | 500 | E99 | 알 수 없는 시스템 에러 |

> `@RestControllerAdvice`는 모든 `@RestController`에서 발생하는 예외를 한 곳에서 처리한다.
> 각 Controller에 try-catch를 반복하지 않아도 되므로 코드가 간결해진다.
