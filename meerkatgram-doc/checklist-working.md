# Meerkatgram 문서 작성 체크리스트

> 직업훈련학원 훈련생 대상 문서. 설계 → 구현 흐름 순서로 작성한다.
> 코드 인용 + 개념 설명 병행. 에러 처리는 `@RestControllerAdvice` 기준.
>
> **파일 저장 위치:** `meerkatgram-doc/1st-doc/` (프로젝트 루트 기준)

---

## 01. 프로젝트 개요 (`1st-doc/01-project-overview.md`) ✅

- [x] 프로젝트 소개 및 목적
- [x] 주요 기능 목록 (인증, 게시글, 파일 업로드)
- [x] 기술 스택 목록 및 선택 이유
- [x] 전체 요청 흐름 개요 (Client → Filter → Controller → Service → Mapper → DB)

---

## 02. ERD & 데이터베이스 (`1st-doc/02-erd-and-database.md`) ✅

- [x] ERD 다이어그램 (Mermaid)
- [x] 테이블별 컬럼 스키마 (타입, 제약조건 포함)
- [x] 테이블 간 관계(FK) 설명
- [x] MyBatis ResultMap 연결 구조 설명

---

## 03. 백엔드 아키텍처 (`1st-doc/03-backend-architecture.md`) ✅

- [x] 레이어드 아키텍처 개념 설명
- [x] 패키지 구조 트리 + 각 패키지 역할 설명
- [x] `domain` vs `global` 분리 이유
- [x] 요청 처리 흐름 (Controller → Service → Mapper)
- [x] 공통 응답 구조 (`GlobalRes`) 설명

---

## 04. API 명세서 (`1st-doc/04-api-specification.md`) ✅

- [x] 공통 요청/응답 형식 정의
- [x] 에러 응답 형식 정의
- [x] **Auth API**
  - [x] 로그인 `POST /api/login`
  - [x] 토큰 재발급 `POST /api/reissue-token`
  - [x] 로그아웃 `POST /api/logout`
- [x] **User API**
  - [x] 유저 조회 `GET /api/users/{id}`
  - [x] 회원가입 `POST /api/users`
- [x] **Post API**
  - [x] 게시글 목록 `GET /api/posts`
  - [x] 게시글 상세 `GET /api/posts/{id}`
  - [x] 게시글 작성 `POST /api/posts`
  - [x] 게시글 삭제 `DELETE /api/posts/{id}`
- [x] **File API**
  - [x] 게시글 이미지 업로드 `POST /api/images/posts`
  - [x] 프로필 이미지 업로드 `POST /api/images/profiles`
- [x] 에러 코드 목록

---

## 05. JWT 인증 구현 가이드 (`1st-doc/05-auth-jwt-guide.md`) ✅

- [x] JWT 개념 (Claim, Signature, 만료 시간)
- [x] Access Token vs Refresh Token 역할 차이
- [x] 인증 흐름 다이어그램 (로그인 → 토큰 발급 → API 요청 → 토큰 갱신)
- [x] `SecurityFilterChain` 설정 코드 인용 및 설명
- [x] `TokenAuthenticationFilter` 코드 인용 및 설명 (토큰 검증 → SecurityContext 등록)
- [x] 로그인 서비스 코드 인용 및 설명 (토큰 생성 흐름)
- [x] `CookieManager` Refresh Token Cookie 처리 코드 인용 및 설명
- [x] 토큰 갱신(`reissue-token`) 흐름 코드 인용 및 설명

---

## 06. 핵심 기능 구현 가이드 (`1st-doc/06-key-features-guide.md`) ✅

- [x] **글로벌 응답/에러 처리**
  - [x] `GlobalRes<T>` DTO 구조 코드 인용 및 설명
  - [x] `GlobalExceptionHandler` (`@RestControllerAdvice`) 코드 인용 및 설명
  - [x] 커스텀 예외 클래스 구조 설명 (`FileStorageException`, `InvalidTokenException`, `NotRegisteredException`)
- [x] **파일 업로드**
  - [x] `LocalFileManager` 코드 인용 및 설명
  - [x] `MultipartFile` 처리 흐름 설명
  - [x] CORS(`CorsConfig`) 및 정적 리소스(`WebConfig`) 설정 설명
- [x] **게시글 CRUD**
  - [x] 게시글 작성/삭제 서비스 코드 인용 및 설명
  - [x] 페이지네이션 구현 (MyBatis `LIMIT/OFFSET`) 설명
  - [x] MyBatis Mapper XML 작성 패턴 설명

---

## 07. 환경 설정 및 실행 가이드 (`1st-doc/07-setup-guide.md`) ✅

- [x] 사전 설치 목록 (JDK 17, MySQL 8.4, Gradle)
- [x] DB 생성 및 스키마 SQL 제공
- [x] `application.yaml` 주요 설정 항목 설명 (실제 값 제외)
- [x] 백엔드 빌드 및 실행 방법
- [x] API 테스트 방법 (curl 예시)
- [x] 트러블슈팅 (자주 발생하는 오류 및 해결법)
