# Meerkatgram 문서 인덱스 (Documentation Index)

이 문서는 Meerkatgram 백엔드 프로젝트 관련 문서들의 구조화된 인덱스를 제공합니다. 아래 링크를 통해 각 구현 세부 사항, 아키텍처 디자인, API 명세 및 개발 환경 설정 가이드를 탐색할 수 있습니다.

---

## ⚠️ 주의사항 (Important Rule)
> [!IMPORTANT]
> - **모든 개발 관련 문서는 기본적으로 한국어로 작성 및 업데이트되어야 합니다.**
> - **더 필요한 정보가 있다고 판단되거나, 부정확한 사항에 대해서는 항상 질문을 통해 해결하고 작업을 진행합니다.**

---

## 🏗️ 프로젝트 구조 및 레이어 역할

### 📁 디렉토리 구조 (Directory Structure)
```
src/main/java/com/msa4meerkatgram/
│
├── Msa4MeerkatgramApplication.java      # 애플리케이션 진입점 (@SpringBootApplication)
│
├── domain/                              # 비즈니스 도메인별 코드
│   ├── auth/                            # 인증 (로그인, 로그아웃, 토큰 재발급)
│   ├── file/                            # 파일 업로드 (프로필, 게시글 이미지)
│   ├── post/                            # 게시글 CRUD 및 페이지네이션
│   └── user/                            # 회원가입, 사용자 조회
│
└── global/                              # 전 도메인 공통 설정 및 코드
    ├── config/                          # CORS 및 WebMvc 정적 리소스 설정
    ├── errors/                          # 글로벌 예외 처리 (@RestControllerAdvice)
    ├── responses/                       # 공통 응답 포맷 (GlobalRes)
    ├── security/                        # Spring Security 및 JWT 인증 설정/필터
    └── util/                            # 로컬 파일 매니저 등 유틸리티 클래스
```

### 🧱 레이어별 역할 (Layer Roles)

이 프로젝트는 **레이어드 아키텍처 (Layered Architecture)** 규칙을 따르며, 각 레이어의 인접한 계층 간의 단방향 통신을 지향합니다.

| 레이어 | 주요 역할 및 책임 | 작성 위치 및 규칙 |
| :--- | :--- | :--- |
| **Filter Layer (Spring Security)** | - 클라이언트 요청 검증 (JWT 토큰 검증)<br>- 인증 정보 `SecurityContext` 등록<br>- 인증/인가 실패 예외 처리 | `global/security/filter/`<br>비즈니스 로직을 처리하지 않습니다. |
| **Controller Layer (API)** | - HTTP 요청 수신 및 응답 반환<br>- `@Valid`를 활용한 요청 데이터(DTO) 유효성 검증<br>- 공통 응답 포맷(`GlobalRes`) 래핑 및 전송 | `domain/*/controllers/`<br>비즈니스 로직을 직접 수행하지 않고 Service에 위임합니다. |
| **Service Layer (Business)** | - 핵심 비즈니스 요구사항 및 시나리오 구현<br>- 데이터 정합성을 위한 트랜잭션 관리 (`@Transactional`)<br>- DB 엔티티를 클라이언트 응답용 Response DTO로 변환 | `domain/*/services/`<br>HTTP 종속적인 코드(HttpServletRequest 등)는 작성하지 않습니다. |
| **Mapper Layer (MyBatis)** | - 데이터베이스 접근 및 SQL 쿼리 실행<br>- SQL 실행 결과를 Java 객체(Entity)로 변환 | 인터페이스: `domain/*/mapper/`<br>XML 쿼리: `src/main/resources/mapper/` |

### 📦 데이터 모델 분류 (Data Model Category)

- **Entity**: 데이터베이스 테이블 스키마와 1:1로 매핑되는 도메인 모델 객체입니다.
- **Request DTO (Java Record)**: 클라이언트의 API 요청 바디 데이터를 나타내며, `record`로 구현되어 데이터 위변조를 막는 불변 객체입니다.
- **Response DTO**: 보안상 감춰야 하는 필드(비밀번호, 토큰 등)를 제외하고, 화면에 표현할 필요한 데이터만 필터링하여 제공하기 위해 `Builder` 패턴을 사용하여 생성하는 객체입니다.

