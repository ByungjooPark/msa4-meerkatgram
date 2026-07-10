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
@Bean
public SecurityFilterChain filterChain(HttpSecurity http,
    SecurityExceptionHandler securityExceptionHandler,
    TokenAuthenticationFilter tokenAuthenticationFilter) throws Exception {

    return http
        // ① 세션을 사용하지 않음 (JWT는 무상태)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // ② SSR 방식이 아니므로 화면 관련 기능 모두 비활성화
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        // ③ CORS 설정 적용
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        // ④ 커스텀 JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 삽입
        .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        // ⑤ URL별 인증 요구 설정
        .authorizeHttpRequests(req ->
            req.requestMatchers(HttpMethod.GET, SecurityUrlRegistry.AUTH_REQUIRED_GET_URLS).authenticated()
               .requestMatchers(HttpMethod.POST, SecurityUrlRegistry.AUTH_REQUIRED_POST_URLS).authenticated()
               .requestMatchers(HttpMethod.PATCH, SecurityUrlRegistry.AUTH_REQUIRED_PATCH_URLS).authenticated()
               .requestMatchers(HttpMethod.DELETE, SecurityUrlRegistry.AUTH_REQUIRED_DELETE_URLS).authenticated()
               .anyRequest().permitAll()
        )
        // ⑥ 인증/인가 실패 시 처리할 핸들러 등록
        .exceptionHandling(e -> e
            .authenticationEntryPoint(securityExceptionHandler)   // 401 처리
            .accessDeniedHandler(securityExceptionHandler)        // 403 처리
        )
        .build();
}
```

---

## 5. `SecurityUrlRegistry` — 인증 필요 URL 목록

```java
// SecurityUrlRegistry.java
public final class SecurityUrlRegistry {
    private SecurityUrlRegistry() {} // 인스턴스화 방지

    public static final String[] AUTH_REQUIRED_GET_URLS    = { "/api/posts/{id}" };
    public static final String[] AUTH_REQUIRED_POST_URLS   = { "/api/logout", "/api/posts" };
    public static final String[] AUTH_REQUIRED_PATCH_URLS  = { "/api/users" };
    public static final String[] AUTH_REQUIRED_DELETE_URLS = { "/api/posts" };
}
```

> 인증이 필요한 URL을 한 파일에 모아 관리하면, Security 설정(`SecurityConfiguration`)과 URL 목록의 관심사를 분리할 수 있다.

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
        Optional<String> tokenOptional = jwtTokenProvider.extractAccessToken(request);

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
- 토큰이 없어도 예외를 던지지 않는다. 인증 없이 다음 필터로 넘기고, `authorizeHttpRequests`에서 인증 필요 여부를 최종 판단한다
- 토큰이 있지만 오류가 있는 경우만 예외 처리를 한다

---

## 7. `SecurityAuthenticationProvider` — SecurityContext 등록

```java
// SecurityAuthenticationProvider.java
@Component
public class SecurityAuthenticationProvider {
    private final JwtTokenProvider jwtTokenProvider;

    public Authentication authentication(String token) {
        // 토큰 검증 후 Claims 객체를 principal(사용자 정보)로 등록
        return new UsernamePasswordAuthenticationToken(
            jwtTokenProvider.extractClaims(token), // principal = Claims
            null,                                   // credentials (불필요)
            List.of()                               // authorities (권한 목록, 미사용)
        );
    }
}
```

> Controller에서 `@AuthenticationPrincipal Claims claims`로 꺼낼 수 있는 이유가 여기에 있다.
> `UsernamePasswordAuthenticationToken`의 첫 번째 인자(principal)에 `Claims` 객체를 넣었기 때문이다.

---

## 8. `JwtTokenProvider` — 토큰 생성·검증

```java
// JwtTokenProvider.java
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
@Transactional
public AuthRes login(LoginReq loginRequest, HttpServletResponse response) {
    // ① DB에서 이메일로 유저 조회
    User user = userMapper.findByEmail(loginRequest.email());
    if (user == null) {
        throw new NotRegisteredException("아이디와 비밀번호를 확인해주세요.");
    }

    // ② BCrypt로 비밀번호 검증
    if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
        throw new NotRegisteredException("아이디와 비밀번호를 확인해주세요.");
    }

    // ③ 토큰 생성 → DB 저장 → 쿠키 세팅 → 응답 반환
    return generateAuthentication(user, response);
}

private AuthRes generateAuthentication(User user, HttpServletResponse response) {
    String newAccessToken  = jwtTokenProvider.generateAccessToken(user);
    String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

    authMapper.updateRefreshToken(user.getId(), newRefreshToken); // DB 저장
    cookieManager.setCookie(response,
        jwtConfig.refreshTokenCookieName(),
        newRefreshToken,
        jwtConfig.refreshTokenCookieExpiry(),
        jwtConfig.reissUri()
    );

    return AuthRes.builder()
        .accessToken(newAccessToken)
        .user(UserRes.builder()...build())
        .build();
}
```

> 존재하지 않는 이메일과 비밀번호 불일치 모두 같은 메시지(`"아이디와 비밀번호를 확인해주세요"`)를 반환한다.
> 어느 쪽이 틀렸는지 구분해주면 공격자에게 정보를 제공하는 셈이므로, 의도적으로 동일한 메시지를 사용한다.

---

## 11. 토큰 재발급 흐름 (`AuthService.reissue`)

```java
// AuthService.java
@Transactional
public AuthRes reissue(HttpServletRequest request, HttpServletResponse response) {
    // ① 쿠키에서 Refresh Token 추출
    Optional<String> refreshTokenOptional = jwtTokenProvider.extractRefreshToken(request);
    if (refreshTokenOptional.isEmpty()) {
        throw new InvalidTokenException("토큰 미존재");
    }
    String refreshToken = refreshTokenOptional.get();

    // ② Refresh Token에서 userId 파싱
    long id = Long.parseLong(jwtTokenProvider.extractClaims(refreshToken).getSubject());

    // ③ DB에서 유저 조회
    User user = userMapper.findByPk(id);
    if (user == null) {
        throw new InvalidTokenException("유효하지 않은 회원의 토큰입니다.");
    }

    // ④ DB에 저장된 Refresh Token과 비교 (탈취 감지)
    if (!refreshToken.equals(user.getRefreshToken())) {
        throw new InvalidTokenException("토큰이 일치하지 않습니다.");
    }

    // ⑤ 새 토큰 쌍 발급
    return generateAuthentication(user, response);
}
```

> ④번 단계(DB 비교)가 핵심이다. 공격자가 탈취한 Refresh Token으로 재발급을 시도해도,
> 서버 측에서 DB 값을 `NULL`로 바꾸거나 다른 토큰으로 교체하면 즉시 차단할 수 있다.
