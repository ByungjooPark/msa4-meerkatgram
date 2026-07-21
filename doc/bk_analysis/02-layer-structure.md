# 레이어 구성 (Layer Structure)

이 프로젝트는 **레이어드 아키텍처(Layered Architecture)** 를 따르며, 상위 레이어는 하위 레이어에만 의존하는 단방향 흐름을 지향합니다.

```
Filter (Spring Security)
   ↓
Controller
   ↓
Service
   ↓
Repository (Spring Data JPA / QueryDSL)
   ↓
Entity ←→ MySQL
```

## 1. Filter Layer — `global/security/filter/`

Spring Security의 필터 체인 레벨. 비즈니스 로직을 다루지 않고 **인증(누구인지 확인)** 만 담당합니다.

| 클래스 | 역할 |
|---|---|
| `SecurityConfiguration` | `SecurityFilterChain` 빈 정의. Stateless 세션, CSRF/폼로그인/기본인증 비활성화, CORS 설정 연결, URL별 인증 요구사항 등록 |
| `TokenAuthenticationFilter` | `OncePerRequestFilter` 구현. Authorization 헤더에서 Bearer 토큰을 꺼내 검증하고 `SecurityContext`에 인증 정보 등록 |
| `SecurityAuthenticationProvider` | JWT의 `Claims`를 담은 `UsernamePasswordAuthenticationToken` 생성 (권한 리스트는 항상 빈 값) |
| `SecurityExceptionHandler` | 인증 실패(401)/인가 실패(403) 시 `HandlerExceptionResolver`로 위임하여 `GlobalExceptionHandler`와 같은 응답 포맷 유지 |
| `SecurityUrlRegistry` | HTTP Method별로 "인증이 필요한 URL 목록"을 상수 배열로 관리 (화이트리스트가 아닌 블랙리스트 방식) |

## 2. Controller Layer — `domain/*/controllers/`

- `@RestController` + `@RequestMapping("/api")` + 도메인별 세부 경로.
- HTTP 요청을 받아 `@Valid`로 Request DTO를 검증하고, **모든 처리는 Service에 위임**합니다. 컨트롤러 자체에는 비즈니스 로직(if/else, DB 접근 등)이 없습니다.
- 반환 타입은 항상 `ResponseEntity<GlobalRes<T>>`.
- Swagger 문서화(`@Tag`, `@Operation`, `@ApiResponse`, 커스텀 `@ApiNotValidErrorResponse` 등)를 컨트롤러 메서드에 직접 부착.

| 컨트롤러 | 엔드포인트 | 상태 |
|---|---|---|
| `AuthController` | `/api/login`, `/api/reissue-token`, `/api/logout`, `/api/registration` | 전체 구현 완료 |
| `FileController` | `/api/files/profiles`, `/api/files/posts` | 구현 완료 |
| `PostController` | `/api/posts` (목록), `/api/posts/{id}` (상세) | 목록/상세만 구현. **작성(store) API는 주석 처리되어 비활성 상태** |
| `UserController` | (없음) | `@RequestMapping("/api")`만 있는 빈 클래스. 실질적인 엔드포인트 없음 |

## 3. Service Layer — `domain/*/services/`

- `@Service` + `@RequiredArgsConstructor`(생성자 주입).
- 핵심 비즈니스 로직과 `@Transactional(rollbackFor = Exception.class)` 트랜잭션 경계를 담당.
- `HttpServletRequest`/`Response`를 인자로 받는 경우가 있음 (`AuthService`) — 쿠키 설정 등 인증 특성상 예외적으로 허용된 패턴으로 보이며, 다른 도메인 서비스에는 이런 패턴이 없음.
- Entity를 Response DTO로 변환하는 책임도 이 레이어(또는 Response record의 정적 팩토리 메서드)가 가짐.

| 서비스 | 주요 메서드 | 상태 |
|---|---|---|
| `AuthService` | `login`, `reissue`, `logout`, `registration`, `generateAuthentication`(private) | 완료 |
| `FileService` | `storeProfile`, `storePosts` | 완료 |
| `PostService` | `index`(페이지네이션), `show`(상세) | 완료. `store`는 주석 처리(MyBatis 시절 코드 잔재) |
| `UserService` | (없음, `JwtProvider`만 주입된 빈 클래스) | 미구현 |

## 4. Repository Layer — `domain/*/repositories/`

두 가지 방식이 공존합니다.

1. **Spring Data JPA** (`JpaRepository<Entity, Long>` 상속) — 단순 CRUD, 파생 쿼리 메서드(`findByEmail`, `existsByEmail`, `countByUser` 등)
2. **QueryDSL 전용 Repository** — 조인/페이징 등 복잡한 쿼리가 필요할 때 별도 클래스(`@Repository`)로 분리하고 `JPAQueryFactory`를 주입받아 사용 (`PostQueryRepository`)

| 리포지토리 | 종류 | 비고 |
|---|---|---|
| `AuthRepository` | JpaRepository\<User, Long\> | `findByEmail`, `existsByEmail` |
| `UserRepository` | JpaRepository\<User, Long\> | 파생 메서드 없음. `AuthRepository`와 **동일한 User 엔티티를 대상으로 하는 별개의 리포지토리**가 이미 존재함 |
| `PostRepository` | JpaRepository\<Post, Long\> | `countByUser` |
| `PostQueryRepository` | QueryDSL (`@Repository`) | `pagination(offset, limit)` — 게시글+작성자 fetch join 페이지네이션 |

> `AuthRepository`와 `UserRepository`가 둘 다 `User` 엔티티를 다루는 것은 "인증 도메인은 인증 도메인의 리포지토리를, 유저 도메인은 유저 도메인의 리포지토리를 쓴다"는 의도로 보이나, 실질적으로 같은 테이블에 대한 접근점이 두 개로 나뉘는 구조입니다.

## 5. Entity Layer — `domain/*/entities/`

- `@Entity` + `@Getter`/`@Setter`(Lombok) — **불변이 아닌, setter를 통한 가변 객체**.
- 소프트 삭제 패턴: `@SQLDelete(sql = "UPDATE ... SET deleted_at = NOW() ...")` + `@SQLRestriction("deleted_at IS NULL")` 를 사용해 `delete()` 호출 시 실제로는 UPDATE가 실행되고, 이후 모든 조회에는 자동으로 `deleted_at IS NULL` 조건이 붙습니다.
- 생성/수정 시각은 `@CreatedDate`/`@LastModifiedDate` + `@EntityListeners(AuditingEntityListener.class)`로 자동 관리 (`Msa4MeerkatgramApplication`의 `@EnableJpaAuditing` 필요).
- PK는 `BIGINT UNSIGNED` + `GenerationType.IDENTITY`.
- `Post → User`는 단방향 `@ManyToOne(FetchType.LAZY)`이며 실제 DB 외래키 제약조건은 생성하지 않음(`ConstraintMode.NO_CONSTRAINT`). `User → Post`(`@OneToMany`)는 주석 처리되어 있어 현재 양방향 매핑은 사용하지 않습니다.

## 6. Request / Response DTO Layer

- **Request DTO**: `record`로 구현하고, 필드에 Jakarta Validation 애노테이션(`@NotBlank`, `@Pattern`, `@Min` 등)을 붙입니다. 필요 시 커스텀 캐노니컬 생성자(기본값 설정: `PostIndexReq`)나 `@AssertTrue` 교차 필드 검증(`RegistrationReq.isPasswordMatch()`)을 사용합니다.
- **Response DTO**: 대부분 `record` + `static from(Entity ...)` 팩토리 메서드 패턴. 일부는 Lombok `@Builder`를 병행 사용(`FileRes`) — 두 스타일이 혼재되어 있습니다.
- Response DTO는 Entity를 그대로 노출하지 않고 필요한 필드만 선별하여 응답합니다(비밀번호, 리프레시 토큰 등 민감 정보 제외).

## 7. Global(공통) 레이어 — `global/`

도메인에 속하지 않는 횡단 관심사(Cross-cutting concern)를 모아둔 영역입니다.

| 하위 패키지 | 역할 |
|---|---|
| `config/` | CORS 설정값 바인딩, WebMvc 정적 리소스 매핑, QueryDSL `JPAQueryFactory` 빈 등록, OpenAPI 문서 메타정보 |
| `errors/` | 커스텀 예외 클래스 + `GlobalExceptionHandler`(`@RestControllerAdvice`) |
| `responses/` | 공통 응답 포맷 `GlobalRes<T>`, 응답 코드 enum `CustomResponseCode` |
| `security/` | JWT 발급/검증(`JwtProvider`, `JwtConfig`), 쿠키 관리(`CookieManager`), Security 필터 체인 전반, Provider/Role 상수 enum |
| `util/file/` | 로컬 파일 저장 유틸(`LocalFileManager`, `FileConfig`) |
| `annotations/openapi/` | Swagger 공통 에러 응답을 메타 애노테이션으로 재사용(`@ApiNotValidErrorResponse` 등) |

## 8. 도메인별 구현 완성도 요약

| 도메인 | Controller | Service | Repository | Entity | 종합 상태 |
|---|:-:|:-:|:-:|:-:|---|
| auth | ✅ | ✅ | ✅ | (User 엔티티 재사용) | 완료 |
| file | ✅ | ✅ | 없음(DB 미사용) | 없음 | 완료 |
| post | 목록/상세만 | 목록/상세만 | ✅ | ✅ | 작성(store) 기능 미완성 |
| user | 빈 클래스 | 빈 클래스 | ✅ | ✅ | 엔티티/리포지토리만 존재, API 미구현 |
