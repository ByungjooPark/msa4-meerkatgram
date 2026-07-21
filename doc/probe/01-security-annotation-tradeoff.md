# 시큐리티 인가 방식 트레이드오프: URL Registry(현재) vs 엔드포인트별 어노테이션

> `00-current-state-survey.md` 5번 항목(컨트롤러 & 엔드포인트)에서 언급한
> "인증 필요 여부는 엔드포인트별 어노테이션이 아니라 `SecurityUrlRegistry`에서 중앙관리"에 대한 심화 조사.

## 현재 방식 (코드 확인 완료)

`SecurityUrlRegistry.java`(`global/security/filter/`)는 HTTP 메서드별 `String[]` 5개(GET/POST/PUT/PATCH/DELETE)에
"인증이 필요한 URL 패턴"만 나열하는 **블랙리스트 방식**이다.

```java
public static final String[] AUTH_REQUIRED_GET_URLS = { "/api/posts/{id}" };
public static final String[] AUTH_REQUIRED_POST_URLS = { "/api/logout", "/api/posts" };
public static final String[] AUTH_REQUIRED_PUT_URLS = { };
public static final String[] AUTH_REQUIRED_PATCH_URLS = { };
public static final String[] AUTH_REQUIRED_DELETE_URLS = { "/api/posts/{id}" };
```

`SecurityConfiguration.filterChain()`이 이 배열들을 `requestMatchers(method, urls...).authenticated()` +
`anyRequest().permitAll()`로 조립한다. 즉 **기본값은 공개(permitAll)**이고, 예외적으로 인증이 필요한 URL만
별도 파일에 등록하는 구조다.

## 항목별 비교

| 항목 | 현재 방식 (URL Registry) | 엔드포인트별 어노테이션 (`@PreAuthorize`/`@Secured` 등) |
|---|---|---|
| **기본 안전성(fail-safe)** | **기본값이 공개(permitAll)** — 새 엔드포인트를 만들고 Registry 등록을 깜빡하면 조용히 인증 없이 노출됨 (fail-open) | 기본값을 `denyAll`/`authenticated`로 두고 필요한 곳만 `permitAll` 선언하는 설계가 가능 (fail-closed로 만들기 쉬움) |
| **정책-구현 동기화** | 이미 실제로 drift 발생 확인됨: `AUTH_REQUIRED_POST_URLS`에 `/api/posts`, `AUTH_REQUIRED_DELETE_URLS`에 `/api/posts/{id}`가 등록돼 있지만 해당 컨트롤러 메서드는 주석처리/미구현 상태 — 정책 파일과 실제 코드가 따로 관리되며 어긋난 사례가 이미 이 코드베이스에 존재 | 어노테이션이 메서드 위에 바로 붙으므로, 메서드가 삭제/주석되면 정책도 함께 사라짐 — 구조적으로 drift 자체가 발생하기 어려움 |
| **가시성(한눈에 감사)** | 인증 정책 전체를 파일 하나에서 조망 가능 (엔드포인트 8개 남짓인 현재 규모에선 장점) | 도메인이 늘어나면(planned: comment/like/notification 등) 정책이 여러 파일에 흩어져 전체 감사가 어려워짐 — grep 의존 |
| **세밀한 인가(역할 기반)** | `authenticated()`만 표현 가능 — 로그인 여부만 체크, role 기반 분기 불가. 실제로 `SecurityAuthenticationProvider`가 `authorities`를 `List.of()`로 비워둬서 `RolePolicy`(NORMAL/SUPER)가 있어도 Spring Security 인가 레벨에서는 전혀 활용되지 않음 | `@PreAuthorize("hasRole('SUPER')")` 등 SpEL로 역할·소유자 검증까지 표현 가능. SUPER 전용 API가 생기면 이 방식 없이는 처리 어려움 |
| **패턴 정밀도/오타 위험** | `"/api/posts/{id}"` 같은 문자열을 손으로 정확히 맞춰야 함 — 컨트롤러의 실제 `@GetMapping` 경로와 별개로 관리되어 오타·불일치 위험 | 어노테이션이 메서드에 직접 붙어 경로 문자열을 별도로 다시 쓸 필요 없음 |
| **테스트 용이성** | 필터체인 레벨 통합 테스트(MockMvc + Security) 필요 | `@WithMockUser` + 컨트롤러 단위 테스트로 간단히 검증 가능 (프로젝트에 `spring-security-test` 의존성 이미 존재) |
| **실행 성능/개입 시점** | 필터 체인 단계에서 컨트롤러 진입 전에 차단 — 오버헤드 최소 | AOP 프록시로 메서드 호출 시점에 체크 — 미세하게 늦지만 실무상 차이 무시 가능 |
| **도입 비용** | 이미 구현돼 있음 | `@EnableMethodSecurity` 활성화 + 각 메서드에 어노테이션 추가하는 마이그레이션 필요 |

## 이 프로젝트 맥락에서의 시사점

- 엔드포인트가 8개 안팎인 지금 규모에서는 Registry 방식이 과한 설계는 아니다.
- 다만 이미 "정책은 등록돼 있는데 구현은 없는" drift가 실증됐고, `RolePolicy.SUPER` 같은 역할 기반 인가가
  코드상 준비만 돼 있고 실제로는 동작 불가능한 상태라는 점은 구조적 한계다.
- `comment`/`like`/`notification` 도메인이 추가되고 SUPER 권한이 필요한 API(예: 신고 처리, 강제 삭제)가
  생기는 시점이 오면, 현재 방식은 역할 기반 분기를 표현할 수 없어 한계에 부딪힌다.
- 절충안: 인증 여부(로그인 O/X)는 현재처럼 필터 레벨에서 중앙관리하고, 역할 기반 세부 인가만
  `@PreAuthorize`로 보완하는 하이브리드도 가능하다.
