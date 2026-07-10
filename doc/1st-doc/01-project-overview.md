# 01. 프로젝트 개요

## 1. 프로젝트 소개

**Meerkatgram**은 사용자가 이미지 게시글을 올리고 소통하는 **커뮤니티형 웹 애플리케이션**이다.

이 문서는 백엔드 API 서버 기준으로 작성되었다.
프론트엔드(Vue 3)와 백엔드(Spring Boot 3)는 분리된 프로젝트로 관리하며, HTTP API로 통신한다.

---

## 2. 주요 기능

| 분류 | 기능 |
|------|------|
| 인증 | 회원가입, 로그인, 로그아웃, Access Token 재발급 |
| 유저 | 유저 정보 조회 |
| 게시글 | 게시글 목록(페이지네이션), 상세 조회, 작성, 삭제 |
| 파일 | 게시글 이미지 업로드, 프로필 이미지 업로드 |

### API 엔드포인트 전체 목록

```
[인증]
POST   /api/login              - 로그인 (Access Token 발급)
POST   /api/reissue-token      - Access Token 재발급
POST   /api/logout             - 로그아웃

[유저]
POST   /api/users              - 회원가입
GET    /api/users/{id}         - 유저 정보 조회

[게시글]
GET    /api/posts              - 게시글 목록 (페이지네이션)
GET    /api/posts/{id}         - 게시글 상세
POST   /api/posts              - 게시글 작성 (인증 필요)
DELETE /api/posts/{id}         - 게시글 삭제 (인증 필요)

[파일]
POST   /api/images/posts       - 게시글 이미지 업로드
POST   /api/images/profiles    - 프로필 이미지 업로드
```

---

## 3. 기술 스택

| 영역 | 기술 | 버전 | 선택 이유 |
|------|------|------|-----------|
| 언어 | Java | 17 | LTS 버전. Records, sealed class 등 현대적 문법 지원 |
| 프레임워크 | Spring Boot | 3.5 | 자동 설정, 내장 서버, 풍부한 생태계 |
| 인증/보안 | Spring Security | (Boot 내장) | 필터 체인 기반 인증 처리 표준 |
| JWT 라이브러리 | jjwt | 0.12.6 | Java 생태계 표준 JWT 라이브러리 |
| ORM/SQL | MyBatis | 3.0.5 | SQL을 직접 제어하면서 Java 객체와 매핑. JPA 대비 SQL 가시성 높음 |
| 데이터베이스 | MySQL | 8.4 | 관계형 DB 표준, 실무 사용률 높음 |
| 유효성 검사 | Spring Validation | (Boot 내장) | 어노테이션 기반 입력값 검증 |
| 코드 간소화 | Lombok | (최신) | getter/setter/생성자 보일러플레이트 제거 |
| 빌드 도구 | Gradle | - | Maven 대비 빠른 빌드, 유연한 DSL |

---

## 4. 전체 요청 처리 흐름

클라이언트가 API를 호출하면 아래 순서로 처리된다.

```
Client (Vue 3 / Postman)
    │
    │  HTTP Request
    ▼
[Spring Security Filter Chain]
    │
    ├─ TokenAuthenticationFilter     ← JWT 토큰 검증, SecurityContext에 사용자 정보 등록
    ├─ SecurityExceptionHandler       ← 인증/인가 실패 시 에러 응답 반환
    │
    ▼
[Controller Layer]                   ← HTTP 요청 수신, 입력값 검증 (@Valid)
    │
    ▼
[Service Layer]                      ← 비즈니스 로직 처리
    │
    ▼
[Mapper Layer (MyBatis)]             ← SQL 실행, 결과 객체 매핑
    │
    ▼
[MySQL Database]
    │
    ▼ (역방향으로 결과 반환)
[Controller]
    │
    │  GlobalRes<T> { code, message, data }
    ▼
Client
```

### 에러 발생 시 흐름

정상 흐름과 달리, 예외가 발생하면 `GlobalExceptionHandler`(`@RestControllerAdvice`)가 가로채서 통일된 에러 응답을 반환한다.

```
Service에서 예외 발생 (예: NotRegisteredException)
    │
    ▼
GlobalExceptionHandler (@RestControllerAdvice)
    │
    │  GlobalRes<T> { code: "에러코드", message: "에러 메시지" }
    ▼
Client
```

### 인증이 필요한 요청 흐름

`POST /api/posts` 처럼 로그인한 사용자만 사용할 수 있는 API는 JWT 검증을 거친다.

```
Client
    │  Authorization: Bearer {accessToken}
    ▼
TokenAuthenticationFilter
    ├─ 토큰 없음 → 401 Unauthorized
    ├─ 토큰 만료 → 401 Unauthorized
    └─ 토큰 유효 → Claims(사용자 정보)를 SecurityContext에 등록
    │
    ▼
Controller (@AuthenticationPrincipal Claims claims)
    │                ↑
    │  claims.getSubject() 로 userId 획득
    ▼
Service → Mapper → DB
```

---

## 5. 공통 응답 형식

모든 API 응답은 아래 구조를 따른다. (상세 내용은 [04-api-specification.md](04-api-specification.md) 참고)

```json
{
  "code": "00",
  "message": "정상 처리",
  "data": { ... }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `code` | String | 처리 결과 코드. `"00"` = 성공 |
| `message` | String | 처리 결과 메시지 |
| `data` | T (제네릭) | 응답 데이터. 없으면 `null` |
