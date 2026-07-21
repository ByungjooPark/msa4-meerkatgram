# 05. JWT 인증 구현 가이드

## 1. JWT 개념

**JWT(JSON Web Token)** 는 서버와 클라이언트 간에 정보를 안전하게 전달하기 위한 토큰 형식이다.
`.`으로 구분된 세 파트로 구성된다.

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9   ← Header
.eyJzdWIiOiIxIiwicm9sZSI6Ik5PUk1BTCJ9  ← Payload (Claims)
.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV    ← Signature
```

| 파트 | 설명 |
|------|------|
| **Header** | 알고리즘 타입 (`HS256`) 및 토큰 타입 (`JWT`) |
| **Payload** | 실제 데이터(Claims). `sub`(userId), `role`, `iss`(발급자), `exp`(만료시각) 등 |
| **Signature** | Header + Payload를 비밀 키로 서명한 값. 위조 여부 검증에 사용 |

> Payload는 Base64로 인코딩되어 있을 뿐, 암호화가 아니다. 민감한 정보(`password` 등)는 절대 넣으면 안 된다.

---

## 2. Access Token vs Refresh Token

이 프로젝트는 두 가지 토큰을 함께 사용한다.

| 구분 | Access Token | Refresh Token |
|------|-------------|---------------|
| 역할 | API 인증 | Access Token 재발급 |
| 저장 위치 | 클라이언트 메모리 (JS 변수) | `HttpOnly` 쿠키 + DB |
| 만료 시간 | 짧음 (예: 1시간) | 김 (예: 7일) |
| 전송 방식 | `Authorization: Bearer ...` 헤더 | 쿠키 자동 전송 |
| 탈취 시 위험 | 만료까지 유효 | DB 무효화로 즉시 차단 가능 |

**두 토큰을 함께 쓰는 이유:**
- Access Token만 쓰면: 만료 시간을 길게 설정해야 해서 탈취 위험이 크다
- Refresh Token을 DB에도 저장하는 이유: 로그아웃 또는 강제 만료 시 DB 값을 `NULL`로 바꿔 토큰을 서버 측에서 무효화할 수 있다

---

## 3. 인증 흐름 다이어그램

### 3-1. 로그인 및 토큰 발급

```
Client                          Server
  │                               │
  │  POST /api/login              │
  │  { email, password } ────────►│
  │                               │ 1. DB에서 이메일로 유저 조회
  │                               │ 2. BCrypt 비밀번호 검증
  │                               │ 3. Access Token 생성
  │                               │ 4. Refresh Token 생성 → DB 저장
  │                               │ 5. Refresh Token → HttpOnly 쿠키 Set
  │◄──────────────────────────────│
  │  { accessToken, user }        │
  │  Set-Cookie: refresh_token=.. │
```

### 3-2. 인증이 필요한 API 요청

```
Client                          TokenAuthenticationFilter       Controller
  │                               │                               │
  │  POST /api/posts              │                               │
  │  Authorization: Bearer {AT} ─►│                               │
  │                               │ 1. Authorization 헤더에서 토큰 추출
  │                               │ 2. 서명 검증 + 만료 확인
  │                               │ 3. Claims → SecurityContext 등록
  │                               │──────────────────────────────►│
  │                               │                               │ 4. @AuthenticationPrincipal로
  │                               │                               │    Claims(userId 등) 수신
  │◄──────────────────────────────────────────────────────────────│
  │  응답                          │                               │
```

### 3-3. Access Token 만료 → 재발급

```
Client                          Server
  │                               │
  │  POST /api/reissue-token      │
  │  Cookie: refresh_token={RT} ─►│
  │                               │ 1. 쿠키에서 Refresh Token 추출
  │                               │ 2. 토큰에서 userId 파싱
  │                               │ 3. DB의 refresh_token과 비교
  │                               │ 4. 일치하면 새 토큰 쌍 발급
  │◄──────────────────────────────│
  │  { accessToken, user }        │
  │  Set-Cookie: refresh_token=.. │
```

---

## 4. `SecurityConfiguration` — Filter Chain 설정

```java
// SecurityConfiguration.java
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // ← @PreAuthorize 활성화
@RequiredArgsConstructor
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
        TokenAuthenticationFilter tokenAuthenticationFilter) throws Exception {

        return http
            // ① 세션을 사용하지 않음 (JWT는 무상태)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // ② SSR 방식이 아니므로 화면 관련 기능 모두 비활성화
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            // ③ CORS 설정 적용 (인라인 람다로 직접 구성)
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(corsConfig.allowedOrigins());
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT));
                config.setAllowCredentials(true);
                config.setMaxAge(corsConfig.maxAge());
                return config;
            }))
            // ④ 커스텀 JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 삽입
            .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // ⑤ 필터 체인 레벨에서는 전부 허용 — 인증/인가는 각 컨트롤러 메서드의 @PreAuthorize가 담당
            .authorizeHttpRequests(req -> req.anyRequest().permitAll())
            .build();
    }
}
```

> URL 패턴별로 인증 요구사항을 나열하던 `authorizeHttpRequests` 설정과, 401/403을 처리하던
> `exceptionHandling(...)` 핸들러 등록이 사라졌다. 인가 판단 자체를 필터 체인에서 메서드 레벨
> `@PreAuthorize`로 옮겼기 때문이다 (아래 5번 항목 참고).

---

## 5. 메서드 시큐리티(`@PreAuthorize`) — 엔드포인트별 인가 설정

인증/인가 필요 여부를 URL 목록 하나로 중앙관리하던 `SecurityUrlRegistry`는 제거되었다. 대신
`SecurityConfiguration`에 `@EnableMethodSecurity(prePostEnabled = true)`를 선언하고, 각 컨트롤러
메서드에 `@PreAuthorize`를 직접 붙여 인증/역할(Role) 조건을 표현한다.

```java
// AuthController.java
@PreAuthorize("isAuthenticated()")
@PostMapping("/logout")
public ResponseEntity<GlobalRes<Void>> logout(...) { ... }

// PostController.java
@PreAuthorize("hasAnyRole('SUPER', 'NORMAL')")
@GetMapping("/posts/{id}")
public ResponseEntity<GlobalRes<PostWithUserRes>> show(...) { ... }

@PreAuthorize("hasRole('SUPER')")
@PostMapping("/posts")
public ResponseEntity<GlobalRes<PostWithUserRes>> store(...) { ... }
```

| 엔드포인트 | `@PreAuthorize` | 의미 |
|---|---|---|
| `POST /api/logout` | `isAuthenticated()` | 로그인 여부만 확인 |
| `GET /api/posts/{id}` | `hasAnyRole('SUPER', 'NORMAL')` | 현재 존재하는 Role이 이 둘뿐이라 사실상 로그인 여부만 확인하는 것과 동일 |
| `POST /api/posts` | `hasRole('SUPER')` | **NORMAL 사용자는 게시글을 작성할 수 없다** — 이전(URL Registry, "인증 필요"만 확인)과 달라진 부분(`04-api-specification.md` 6-3 참고) |

> `hasRole(x)`는 내부적으로 `"ROLE_" + x` 형태의 authority를 찾는다(`hasAuthority`는 접두사 없이 그대로 비교).
> 그래서 `hasAuthority('SUPER')`/`hasAnyAuthority(...)`에서 `hasRole('SUPER')`/`hasAnyRole(...)`로 바꾸면서,
> `SecurityAuthenticationProvider`가 만드는 authority 값도 `"SUPER"`에서 `"ROLE_SUPER"`로 함께 바꿔야 했다(7번 항목 참고).

> `@PreAuthorize`가 없는 엔드포인트(`GET /api/posts`, `/api/login`, `/api/reissue-token`, `/api/registration`)는
> 필터 체인 설정(`anyRequest().permitAll()`)이 그대로 적용되어 인증 없이 호출 가능하다.

**인가 실패 시 401/403 분기 (`GlobalExceptionHandler`)**

필터 체인이 전부 `permitAll`이 되면서, 인증되지 않은 요청도 필터 단계가 아니라 `@PreAuthorize` 평가
시점(AOP)에서 걸린다. 이때 Spring Security는 "미로그인"과 "역할 부족"을 구분하지 않고 동일하게
`AccessDeniedException`을 던진다 — 이전에 있던 `AuthenticationException` 전용 핸들러는 더 이상
호출되지 않는다. 그래서 `GlobalExceptionHandler.accessDeniedHandle()`이 `SecurityContext`의 인증
객체를 직접 검사해서 401/403을 나눈다.

```java
// GlobalExceptionHandler.java
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<GlobalRes<Void>> accessDeniedHandle(AccessDeniedException e) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    // 로그인하지 않은 익명 사용자가 접근한 경우 (인증 실패 - 401)
    if (authentication instanceof AnonymousAuthenticationToken) {
        return this.generateErrorResponse(CustomResponseCode.UNAUTHENTICATED_ERROR); // E02
    }

    // 로그인은 했으나 권한(Role)이 부족한 경우 (인가 실패 - 403)
    return this.generateErrorResponse(CustomResponseCode.UNAUTHORIZED_ERROR); // E03
}
```

> 토큰이 없는 요청에는 Spring Security가 기본으로 `AnonymousAuthenticationToken`을 채워 넣는다.
> 그 타입 여부로 "로그인 안 함(401)"과 "로그인은 했지만 역할 부족(403)"을 구분할 수 있는 것이다.

---

## 6. `TokenAuthenticationFilter` — JWT 검증 필터

```java
// TokenAuthenticationFilter.java
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // ① Authorization 헤더에서 Access Token 추출
        Optional<String> tokenOptional = jwtProvider.extractAccessToken(request);

        // ② 토큰이 있을 때만 검증 실행 (없으면 인증 없이 다음 필터로)
        if (tokenOptional.isPresent()) {
            try {
                // ③ 토큰 검증 → Claims 추출 → SecurityContext에 인증 정보 등록
                SecurityContextHolder.getContext().setAuthentication(
                    securityAuthenticationProvider.authentication(tokenOptional.get())
                );
            } catch (Exception e) {
                // ④ 토큰 오류 발생 시 HandlerExceptionResolver로 위임
                //    → GlobalExceptionHandler의 @ExceptionHandler가 처리
                handlerExceptionResolver.resolveException(request, response, null, e);
                return; // 필터 체인 중단 (응답 중복 방지)
            }
        }

        filterChain.doFilter(request, response); // 다음 필터 호출
    }
}
```

**포인트:**
- `OncePerRequestFilter`: 하나의 HTTP 요청에서 단 한 번만 실행되도록 보장
- 토큰이 없어도 예외를 던지지 않는다. 인증 없이 다음 필터로 넘기고, 인증/역할 필요 여부는 각 컨트롤러 메서드의 `@PreAuthorize`가 최종 판단한다
- 토큰이 있지만 오류가 있는 경우만 예외 처리를 한다

---

## 7. `SecurityAuthenticationProvider` — SecurityContext 등록

```java
// SecurityAuthenticationProvider.java
@Component
public class SecurityAuthenticationProvider {
    private final JwtProvider jwtProvider;

    public Authentication authentication(String token) {
        Claims claims = jwtProvider.extractClaims(token);

        // 토큰 검증 후 Claims 객체를 principal(사용자 정보)로 등록
        return new UsernamePasswordAuthenticationToken(
            claims,                                  // principal = Claims
            null,                                    // credentials (불필요)
            getAuthorityFromClaims(claims)            // authorities (Claims의 role 클레임에서 추출)
        );
    }

    private List<SimpleGrantedAuthority> getAuthorityFromClaims(Claims claims) {
        Object role = claims.get("role");
        if (role != null) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + role.toString()));
        }
        return List.of();
    }
}
```

> Controller에서 `@AuthenticationPrincipal Claims claims`로 꺼낼 수 있는 이유가 여기에 있다.
> `UsernamePasswordAuthenticationToken`의 첫 번째 인자(principal)에 `Claims` 객체를 넣었기 때문이다.
> 이전에는 `authorities`가 항상 빈 `List.of()`였지만, 이제 JWT의 `role` 클레임을 `SimpleGrantedAuthority`로
> 변환해 넣는다 — `@PreAuthorize("hasRole('SUPER')")` 같은 역할 기반 인가가 이 값으로 동작한다.
> `"ROLE_"` 접두사를 직접 붙이는 이유: `hasRole(x)`는 내부적으로 authority 문자열이 `"ROLE_" + x`인지
> 비교하기 때문에, 접두사 없이 `"SUPER"`만 넣으면 `hasRole('SUPER')`가 항상 실패한다.

---

## 8. `JwtProvider` — 토큰 생성·검증

```java
// JwtProvider.java
private String generateToken(User user, long ttl) {
    Date now = new Date();
    return Jwts.builder()
        .header().type(jwtConfig.type())    // typ: JWT
        .and()
        .subject(String.valueOf(user.getId())) // sub: userId
        .issuer(jwtConfig.issuer())            // iss: 발급자
        .issuedAt(now)                         // iat: 발급 시각
        .expiration(new Date(now.getTime() + ttl)) // exp: 만료 시각
        .claim("role", user.getRole())         // 커스텀 클레임
        .signWith(this.secretKey)              // HMAC-SHA 서명
        .compact();
}

// 토큰 검증 및 Claims 추출
public Claims extractClaims(String token) {
    try {
        return Jwts.parser()
            .verifyWith(this.secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    } catch (ExpiredJwtException e) {
        throw new InvalidTokenException("토큰이 만료되었습니다.");
    } catch (UnsupportedJwtException e) {
        throw new InvalidTokenException("서명이 위조된 유효하지 않은 토큰입니다.");
    } catch (MalformedJwtException e) {
        throw new InvalidTokenException("토큰 형식이 올바르지 않습니다.");
    } catch (JwtException | IllegalArgumentException e) {
        throw new InvalidTokenException("인증 토큰 검증에 실패했습니다.");
    }
}
```

---

## 9. `CookieManager` — Refresh Token 쿠키 처리

```java
// CookieManager.java
public void setCookie(HttpServletResponse response, String name, String value, int maxAge, String path) {
    Cookie cookie = new Cookie(name, value);
    cookie.setPath(path);
    cookie.setMaxAge(maxAge);
    cookie.setHttpOnly(true);              // ① JS에서 접근 불가 → XSS 공격 방지
    cookie.setSecure(jwtConfig.secure());  // ② HTTPS 환경에서만 전송 → MITM 공격 방지
    response.addCookie(cookie);
}
```

| 속성 | 값 | 보안 효과 |
|------|----|-----------|
| `HttpOnly` | true | JavaScript(`document.cookie`)에서 접근 불가 → XSS 방어 |
| `Secure` | 설정값 | HTTPS 연결에서만 쿠키 전송 → 평문 도청 방어 |
| `Path` | `/api/reissue-token` | 재발급 경로에서만 쿠키 전송 → 불필요한 노출 최소화 |
| `maxAge` | 0 | 쿠키 즉시 삭제 (로그아웃 시 사용) |

---

## 10. 로그인 서비스 흐름 (`AuthService.login`)

```java
// AuthService.java
@Transactional(rollbackFor = Exception.class)
public AuthRes login(HttpServletResponse response, LoginReq loginReq) {
    // ① DB에서 이메일로 유저 조회 + 가입 여부 확인
    User user = authRepository.findByEmail(loginReq.email())
        .orElseThrow(() -> new NotRegisteredException("아이디와 비밀번호를 확인해주세요."));

    // ② BCrypt로 비밀번호 검증
    if (!passwordEncoder.matches(loginReq.password(), user.getPassword())) {
        throw new NotRegisteredException("아이디와 비밀번호를 확인해주세요.");
    }

    // ③ 토큰 생성 → DB 저장 → 쿠키 세팅 → 응답 반환
    return this.generateAuthentication(response, user);
}

private AuthRes generateAuthentication(HttpServletResponse response, User user) {
    // 작성 게시글 수 획득 (로그인 응답에 함께 내려줌)
    long countPosts = postRepository.countByUser(user);

    String newAccessToken  = jwtProvider.generateAccessToken(user);
    String newRefreshToken = jwtProvider.generateRefreshToken(user);

    // 리프레시 토큰을 DB에 저장
    user.setRefreshToken(newRefreshToken);
    authRepository.save(user);

    // 리프레시 토큰을 쿠키에 저장
    cookieManager.setCookie(
        response
        , jwtConfig.refreshTokenCookieName()
        , newRefreshToken
        , jwtConfig.refreshTokenCookieExpiry()
        , jwtConfig.reissueUri()
    );

    return AuthRes.from(user, newAccessToken, countPosts);
}
```

> 존재하지 않는 이메일과 비밀번호 불일치 모두 같은 메시지(`"아이디와 비밀번호를 확인해주세요"`)를 반환한다.
> 어느 쪽이 틀렸는지 구분해주면 공격자에게 정보를 제공하는 셈이므로, 의도적으로 동일한 메시지를 사용한다.
> `generateAuthentication`은 로그인/재발급이 공유하는 private 메서드다.

---

## 11. 토큰 재발급 흐름 (`AuthService.reissue`)

```java
// AuthService.java
@Transactional(rollbackFor = Exception.class)
public AuthRes reissue(HttpServletRequest request, HttpServletResponse response) {
    // ① 쿠키에서 Refresh Token 추출
    String extractRefreshToken = jwtProvider.extractRefreshToken(request)
        .orElseThrow(() -> new InvalidTokenException("토큰이 없습니다."));

    // ② Refresh Token에서 userId 파싱
    long id = Long.parseLong(jwtProvider.extractClaims(extractRefreshToken).getSubject());

    // ③ DB에서 유저 조회
    User user = authRepository.findById(id)
        .orElseThrow(() -> new InvalidTokenException("유효하지 않은 회원의 토큰입니다."));

    // ④ 비로그인 상태 확인 (로그아웃된 유저는 refreshToken이 null)
    if (user.getRefreshToken() == null) {
        throw new InvalidTokenException("유효하지 않은 회원의 토큰입니다.");
    }

    // ⑤ DB에 저장된 Refresh Token과 비교 (탈취 감지)
    if (!user.getRefreshToken().equals(extractRefreshToken)) {
        throw new InvalidTokenException("토큰이 일치하지 않습니다.");
    }

    // ⑥ 새 토큰 쌍 발급
    return this.generateAuthentication(response, user);
}
```

> ④번 단계(DB 비교)가 핵심이다. 공격자가 탈취한 Refresh Token으로 재발급을 시도해도,
> 서버 측에서 DB 값을 `NULL`로 바꾸거나 다른 토큰으로 교체하면 즉시 차단할 수 있다.
