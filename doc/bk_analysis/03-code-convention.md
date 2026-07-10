# 코드 컨벤션 (Code Convention)

실제 코드에서 반복적으로 관찰되는 규칙을 정리한 것으로, 팀 내 명시적 컨벤션 문서는 아직 없습니다. 새 코드를 작성할 때는 아래 패턴을 따르는 것이 기존 코드와의 일관성을 유지합니다.

## 1. 패키지 / 네이밍 규칙

- 패키지 구조: `domain.{도메인명}.{계층}` (예: `domain.post.controllers`, `domain.post.services`)
- 계층 하위 패키지명은 **복수형**: `controllers`, `services`, `repositories`, `entities`, `requests`, `responses`
- 클래스 접미사(Suffix) 규칙:
  - 컨트롤러: `XxxController`
  - 서비스: `XxxService`
  - 리포지토리: `XxxRepository` (QueryDSL 전용은 `XxxQueryRepository`)
  - 요청 DTO: `XxxReq`
  - 응답 DTO: `XxxRes`
  - 커스텀 예외: `XxxException`
  - 설정값 바인딩 클래스: `XxxConfig`
  - 상수/enum: `XxxPolicy` (예: `RolePolicy`, `ProviderPolicy`), `XxxRegistry`(예: `SecurityUrlRegistry`)

## 2. 의존성 주입

- 생성자 주입만 사용하며, 항상 Lombok `@RequiredArgsConstructor` + `private final` 필드로 선언합니다. `@Autowired` 필드 주입은 사용하지 않습니다.

## 3. Entity 작성 규칙

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "테이블명")
@SQLDelete(sql = "UPDATE 테이블명 SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
public class Xxx { ... }
```

- 소프트 삭제가 필요한 테이블은 반드시 `@SQLDelete` + `@SQLRestriction` 세트로 적용.
- 생성/수정 시각은 `@CreatedDate` / `@LastModifiedDate`로 자동화하고 수동으로 세팅하지 않음.
- PK: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` + `columnDefinition = "BIGINT UNSIGNED"`.
- Enum 컬럼: `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(Types.VARCHAR)` 조합.
- 연관관계는 기본적으로 `FetchType.LAZY`, 물리적 FK 제약조건은 `@ForeignKey(ConstraintMode.NO_CONSTRAINT)`로 생략하는 편(애플리케이션 레벨 정합성에 의존).
- Entity는 record가 아닌 **가변 클래스 + Setter**로 작성 (JPA 프록시/변경감지 특성상 record 사용 불가).

## 4. DTO 작성 규칙

- **Request**: `record` + Jakarta Validation 애노테이션. 검증 실패 메시지는 한글로 직접 명시(`message = "..."`.)
  - 필드 간 교차 검증이 필요하면 `@AssertTrue`가 붙은 커스텀 메서드를 record 본문에 추가 (`RegistrationReq.isPasswordMatch()`).
  - 기본값이 필요하면 커스텀 캐노니컬 생성자를 정의 (`PostIndexReq`).
- **Response**: `record` + `public static XxxRes from(Entity entity, ...)` 정적 팩토리 메서드로 Entity → DTO 변환. (`FileRes`만 예외적으로 Lombok `@Builder` 사용 — 신규 코드는 `from()` 팩토리 패턴 쪽을 기본으로 따르는 것을 권장)
- Response는 절대 Entity를 그대로 반환하지 않고, 필요한 필드만 선택적으로 노출.

## 5. 공통 응답 / 예외 컨벤션

- 모든 컨트롤러 반환 타입: `ResponseEntity<GlobalRes<T>>`
- 성공 응답: `GlobalRes.success(data)` 또는 `GlobalRes.success()`(Void)
- 비즈니스 예외는 `RuntimeException`을 상속하는 전용 클래스를 `global/errors/custom/`에 만들고, 메시지는 생성자 인자로 전달.
- 새 예외를 추가하면 반드시 `CustomResponseCode`에 코드를 추가하고 `GlobalExceptionHandler`에 `@ExceptionHandler` 메서드를 추가하는 것이 세트.
- 로그 레벨: 클라이언트 잘못으로 인한 예외(401/400/404/409류)는 `log.debug`, 서버 내부 오류(DB, 시스템 예외)는 `log.error`.

## 6. 설정값(Configuration Properties) 컨벤션

- `@ConfigurationProperties(prefix = "...")`를 **불변 record**로 선언 (`CorsConfig`, `JwtConfig`, `FileConfig`).
- `@ConfigurationPropertiesScan`이 애플리케이션 클래스에 선언되어 있으므로 각 Config record에 별도의 `@Bean`/`@Component` 등록이 필요 없음.

## 7. Swagger(OpenAPI) 문서화 컨벤션

- 컨트롤러 클래스에 `@Tag(name = "... API", description = "...")`.
- 각 엔드포인트 메서드에 `@Operation(summary = "...")`을 붙이고, 에러 응답 문서화는 커스텀 애노테이션 `@CustomApiResponse(value = {CustomResponseCode...})`로 통일합니다. (구버전에 있던 에러 코드별 개별 메타 애노테이션 `@ApiNotValidErrorResponse`/`@ApiUnauthenticatedErrorResponse` 방식은 완전히 대체되어 삭제되었습니다.)
- `@CustomApiResponse`는 `global/config/openapi/CustomApiResponse.java`에 정의된 커스텀 애노테이션으로, **해당 엔드포인트에서 실제로 발생 가능한 `CustomResponseCode` enum 값들을 배열로 나열**하는 방식입니다.
  ```java
  @Operation(summary = "로그인 처리", description = "이메일과 비밀번호로 로그인")
  @CustomApiResponse(value = {
      CustomResponseCode.NOT_REGISTERED_ERROR
      ,CustomResponseCode.INVALID_PARAMETER_ERROR
      ,CustomResponseCode.DB_ERROR
      ,CustomResponseCode.SYSTEM_ERROR
  })
  @PostMapping("/login")
  ```
- 실제 Swagger 문서에 예시 응답을 그려주는 로직은 애노테이션이 아니라 `global/config/openapi/ApiResponseCustomizer`(springdoc의 `OperationCustomizer` 구현체, `@Component`)에 있습니다. 이 커스터마이저가:
  1. 핸들러 메서드에서 `@CustomApiResponse` 애노테이션을 읽고,
  2. 나열된 `CustomResponseCode`들을 **HTTP Status 코드별로 그룹핑**한 뒤,
  3. 그룹별로 `{ "code", "message", "data": null }` 형태의 example을 자동 생성해 OpenAPI `Operation`에 추가합니다.
- 즉 새 엔드포인트를 추가할 때는 "에러 응답 예시를 직접 작성"할 필요 없이, **그 엔드포인트에서 던질 수 있는 `CustomResponseCode` 목록만 `@CustomApiResponse`에 나열**하면 됩니다. (성공 응답 `200 SUCCESS`는 `@CustomApiResponse`에 메타 애노테이션으로 이미 포함되어 있어 별도 표기가 필요 없음.)
- Request DTO(record)의 각 필드에도 `@Schema(description = ..., example = ..., nullable = ..., requiredMode = ...)`를 붙이는 것이 표준이 되었습니다. 교차검증용 메서드(`@AssertTrue`)에는 `@Schema(hidden = true)`를 붙여 Swagger 스키마에는 노출되지 않도록 처리합니다 (`RegistrationReq.isPasswordMatch()`).

## 8. 코드 포맷 스타일

- 인자/필드가 여러 줄일 때 **콤마를 다음 줄 앞에 붙이는 스타일**을 자주 사용:
  ```java
  public AuthController(
      AuthService authService
      , OtherService otherService
  ) { ... }
  ```
- 주석은 한글로, "왜/무엇을 하는지"를 짧게 설명하는 방식(`// 유저정보 획득 + 유저 가입 여부 확인` 등).
- Javadoc(`/** ... */`)은 일부 핵심 메서드(`AuthService.generateAuthentication`, `LocalFileManager` 등)에만 부분적으로 적용됨.

---

## 발견된 비일관성 / 기술부채 (참고용)

코드를 읽으며 확인된, 향후 정리하면 좋을 만한 사항들입니다. **이번 분석 작업 범위에서는 수정하지 않았습니다.**

1. **문서-코드 불일치**: `doc/api_responses.md`는 에러 메시지가 "로그인 에러", "UNAUTHENTICATED_ERROR" 등 한글/설명형 문자열로 내려간다고 명시하지만, 실제 `GlobalRes.from(CustomResponseCode)`는 `customResponseCode.name()`(예: `NOT_REGISTERED_ERROR`, `UNAUTHENTICATED_ERROR`) 을 그대로 `message` 필드에 넣습니다. 즉 일부 에러코드는 enum 상수명이 그대로 노출됩니다. (`ApiResponseCustomizer`가 자동 생성하는 Swagger 예시 응답도 동일하게 `customErrorCode.name()`을 `message`로 사용하므로, 최소한 **Swagger 문서와 실제 런타임 응답 간의 정합성은 이제 확보된 상태**입니다. 다만 `doc/api_responses.md`와의 괴리는 여전히 남아 있습니다.)
2. **미사용(Dead) 코드**:
   - `DuplicatedUserException` — 정의만 있고 실제로 던지는 곳이 없음(`AuthService`는 `DuplicatedRecordException`을 사용).
   - `PostStoreReq`, `PostController`/`PostService`의 게시글 작성(store) 코드 — 전부 주석 처리된 상태(MyBatis 시절 `PostMybatis` 타입을 참조하는 죽은 코드 포함). 다만 `PostStoreReq`는 각 필드에 `@Schema`가 추가되는 등 문서화 관점의 관리는 계속 이루어지고 있어, 조만간 활성화될 가능성이 있어 보입니다.
3. **리포지토리 중복 접근점**: `AuthRepository`와 `UserRepository`가 둘 다 `JpaRepository<User, Long>`를 상속해 동일 테이블에 대한 별도 접근 경로가 두 개 존재합니다.
4. **빈 도메인 클래스**: `UserController`, `UserService`가 실질적인 내용 없이 스켈레톤 상태로 존재합니다.
5. **Role 기반 인가 미연결**: `User.role`(`RolePolicy.NORMAL/SUPER`)이 존재하지만 `SecurityAuthenticationProvider`가 권한 리스트를 항상 빈 값(`List.of()`)으로 채우고, `SecurityConfiguration`도 `hasRole(...)` 등의 세분화된 인가 규칙 없이 `authenticated()` 여부만 검사합니다.
6. **운영 설정 잔재**: `application-prod.yaml`에 이미 사용하지 않는 `mybatis.mapper-locations` 설정이 남아 있습니다.
7. **개발용 기본값 노출**: `application.yaml`에 DB 비밀번호(`msa505`)와 JWT `secret` 기본값이 하드코딩되어 있습니다. 로컬 개발 편의를 위한 것으로 보이나, 운영 배포 시 반드시 환경변수로 덮어써야 합니다.

> ~~Swagger 문서화 커버리지 불균형~~ (해소됨): 이전에는 `AuthController.reissue`/`logout` 등 일부 엔드포인트에 `@Operation`/에러 응답 문서화가 빠져 있었으나, `@CustomApiResponse` + `ApiResponseCustomizer` 도입 이후 `AuthController`/`FileController`/`PostController`의 모든 엔드포인트가 `@Operation` + `@CustomApiResponse`를 일관되게 갖추도록 정리되었습니다. (`UserController`는 API 자체가 아직 없어 해당 없음.)
