# 전체 아키텍처 (Architecture)

## 1. 기술 스택

| 영역 | 기술 | 비고 |
|---|---|---|
| Language | Java 17 | Gradle Toolchain으로 고정 |
| Framework | Spring Boot 3.5.15-SNAPSHOT | `spring.io/snapshot` 저장소 사용 중 (정식 릴리즈 아님, 주의 필요) |
| Web | Spring MVC (`spring-boot-starter-web`) | 서블릿 기반 REST API |
| 인증/인가 | Spring Security + JJWT 0.12.6 | Stateless, 커스텀 JWT 필터 |
| DB 접근 | Spring Data JPA (Hibernate) + QueryDSL 5.1.0 | Repository는 Spring Data + QueryDSL 이원화 |
| DB | MySQL 8.4 (`mysql-connector-j`) | HikariCP 커넥션 풀 |
| API 문서화 | springdoc-openapi 2.8.16 (Swagger UI) | `/swagger-ui.html`, `/api-docs` |
| 검증 | `spring-boot-starter-validation` (Jakarta Bean Validation) | Request DTO(record)에 애노테이션으로 적용 |
| 빌드 | Gradle | `build.gradle` |
| 배포 | Docker (`Dockerfile`), Jenkins (`Jenkinsfile`) | CI/CD 파이프라인 존재 |

프론트엔드는 별도 저장소(Vue 3 추정, README 기준)로 분리되어 있으며, 이 저장소는 **백엔드 API 서버 단독**입니다.

## 2. 전체 요청 처리 흐름

```
[Client(Vue) / Swagger UI]
        │  HTTPS/HTTP + JSON
        ▼
┌───────────────────────────────────────────┐
│ Spring Security Filter Chain               │
│  1) CORS 필터 (CorsConfigurationSource)     │
│  2) TokenAuthenticationFilter (커스텀)      │
│     - Authorization 헤더의 Bearer 토큰 검증 │
│     - 유효하면 SecurityContext에 인증정보 등록│
│  3) (인증 필요 URL만) authorizeHttpRequests │
└───────────────────────────────────────────┘
        ▼
┌───────────────────────────────────────────┐
│ DispatcherServlet → @RestController         │
│  - @Valid 로 요청 DTO 유효성 검증            │
│  - 예외 발생 시 @RestControllerAdvice로 위임 │
└───────────────────────────────────────────┘
        ▼
┌───────────────────────────────────────────┐
│ Service (@Service, @Transactional)          │
│  - 비즈니스 로직, 트랜잭션 경계               │
└───────────────────────────────────────────┘
        ▼
┌───────────────────────────────────────────┐
│ Repository                                  │
│  - JpaRepository (단순 CRUD/파생 쿼리)       │
│  - QueryRepository (QueryDSL 복합 쿼리)      │
└───────────────────────────────────────────┘
        ▼
      MySQL
```

컨트롤러는 항상 `GlobalRes<T>`로 감싼 `ResponseEntity`를 반환하며, 예외는 필터 레벨(`SecurityExceptionHandler`)이든 서비스/컨트롤러 레벨(`GlobalExceptionHandler`)이든 최종적으로 동일한 응답 포맷으로 수렴합니다.

## 3. 패키지 구조

```
src/main/java/com/msa4meerkatgram/
├── Msa4MeerkatgramApplication.java     # 진입점. @EnableJpaAuditing, @ConfigurationPropertiesScan 선언
│
├── domain/                             # 도메인(기능)별 수직 분할
│   ├── auth/        (컨트롤러/리포지토리/요청/응답/서비스) 로그인·로그아웃·토큰재발급·회원가입
│   ├── file/         파일(이미지) 업로드
│   ├── post/         게시글 CRUD (현재 목록/상세만 구현)
│   └── user/         유저 도메인 — 현재 껍데기(스텁)만 존재
│
└── global/                             # 도메인에 종속되지 않는 공통 코드
    ├── annotations/openapi/            # Swagger 공통 에러 응답 애노테이션
    ├── config/                        # CORS, WebMvc 정적 리소스, QueryDSL, OpenAPI 설정
    ├── errors/                        # 전역 예외 처리(@RestControllerAdvice) + 커스텀 예외 클래스
    ├── responses/                     # 공통 응답 포맷(GlobalRes) + 응답 코드(enum)
    ├── security/                      # Spring Security, JWT, 쿠키 관리
    └── util/file/                     # 로컬 파일 저장 유틸
```

`domain/*` 는 다시 `controllers / services / repositories / entities / requests / responses` 하위 패키지로 나뉘는 것이 규칙이나, **모든 도메인이 전체 하위 패키지를 다 갖추고 있지는 않습니다** (예: `user` 도메인은 entity/repository만 있고 controller/service는 빈 껍데기, `file` 도메인은 entity/repository 자체가 없음 — 자세한 도메인별 구현 현황은 [02-layer-structure.md](./02-layer-structure.md) 참고).

## 4. 인증(Authentication) 아키텍처

- **Access Token**: JWT, `Authorization: Bearer <token>` 헤더로 매 요청마다 전달. 서버는 세션을 유지하지 않음(Stateless).
- **Refresh Token**: JWT를 생성해 (1) DB(`users.refresh_token` 컬럼)에 저장, (2) `HttpOnly` + `Secure(설정값)` 쿠키로 클라이언트에 저장. 재발급 시 DB에 저장된 값과 쿠키의 값을 비교해 탈취 여부를 검증.
- 인증 정보 자체는 `SecurityContextHolder`에 JWT의 `Claims` 객체를 그대로 principal로 담습니다(`SecurityAuthenticationProvider`). 별도의 `UserDetails` 구현체는 사용하지 않음.
- 권한(authorities)은 항상 빈 리스트(`List.of()`)로 설정되어 있어, **Spring Security 차원의 Role 기반 인가는 실제로 동작하지 않음**. `User.role`(`RolePolicy.NORMAL/SUPER`) 필드는 존재하지만 인가 로직에는 아직 연결되어 있지 않은 상태입니다.
- URL별 인증 필요 여부는 `SecurityUrlRegistry`에 화이트리스트가 아닌 **블랙리스트(인증이 필요한 URL만 나열)** 방식으로 명시되어 있고, 나머지는 전부 `permitAll()`.

## 5. 예외 처리 / 공통 응답 아키텍처

- 모든 API는 `GlobalRes<T>` (`code`, `message`, `data`) 로 감싸서 반환합니다.
- 성공/실패 코드는 `CustomResponseCode` enum에 `HttpStatus`와 코드 문자열(`00`, `E01` ...)로 정의.
- 비즈니스 예외(로그인 실패, 중복 가입, 삭제된 레코드 등)는 `global/errors/custom/`의 `RuntimeException` 서브클래스로 던지고, `GlobalExceptionHandler`(`@RestControllerAdvice`)가 이를 잡아 `CustomResponseCode`에 맞는 응답으로 변환합니다.
- **필터 단계**(Spring Security)에서 발생하는 인증/인가 예외(`AuthenticationException`, `AccessDeniedException`)는 `@RestControllerAdvice`가 감지할 수 없으므로, `SecurityExceptionHandler`가 `HandlerExceptionResolver`에 예외를 다시 위임해 결과적으로 `GlobalExceptionHandler`가 처리하도록 우회시키는 구조입니다. `TokenAuthenticationFilter`에서 토큰 검증 중 발생하는 예외도 동일한 방식으로 위임됩니다.

## 6. 파일 업로드 아키텍처

- 클라우드 스토리지가 아닌 **로컬 디스크 저장** 방식(`LocalFileManager`, `storage/files/{profiles,posts}`).
- 저장 후 파일은 Spring MVC의 정적 리소스 핸들러(`WebConfig`)를 통해 `/files/**` 경로로 서빙됩니다.
- 파일명은 `yyyyMMdd_UUID.확장자` 형태로 난수화하여 저장(원본 파일명 미사용, 경로 조작/충돌 방지).
- 허용 확장자는 `application.yaml`의 `file.allow-extension-list`로 설정 관리.

## 7. 설정(Configuration) 아키텍처

`@ConfigurationProperties`를 **불변 record**로 선언하는 방식을 일관되게 사용합니다 (`CorsConfig`, `JwtConfig`, `FileConfig`). `Msa4MeerkatgramApplication`에 `@ConfigurationPropertiesScan`이 선언되어 있어 별도의 `@Bean` 등록 없이 자동으로 스캔됩니다.

환경별 설정은 `application.yaml`(공통/로컬 기본값 포함) + `application-prod.yaml`(운영, 값은 전부 환경변수 필수)로 분리되어 있습니다.

> **참고(현재 상태의 특이사항)**: `application-prod.yaml`에는 이미 제거된 MyBatis 관련 설정(`mybatis.mapper-locations`)이 남아 있습니다. 현재 JPA로 전환이 끝난 상태라 이 설정은 사용되지 않는 잔재로 보입니다.
