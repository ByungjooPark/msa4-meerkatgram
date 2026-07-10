# Meerkatgram 백엔드 분석 문서

이 디렉토리(`doc/analysis/`)는 2026-07-10 기준, `feature/v2/migration-jpa` 브랜치의 **실제 소스 코드를 직접 읽고 작성한 분석 문서**입니다.

## ⚠️ 먼저 알아야 할 것

- 프로젝트 루트의 `README.md`, `AGENTS.md`는 **MyBatis 기반 구버전** 설명입니다. (MyBatis Mapper 레이어, `meerkatgram-doc/` 경로 등 언급)
- 실제 코드는 이미 **JPA + QueryDSL**로 전환 완료된 상태입니다. (`git log`상 "JPA 전환작업중", "remove Mybatis System add JPA & QueryDSL System" 커밋 확인)
- 즉 기존 문서와 실제 구현 사이에 괴리가 있으며, 이 분석 문서는 그 괴리를 반영해 **현재 코드가 실제로 어떻게 동작하는지**를 기준으로 작성했습니다.

## 문서 구성

| 문서 | 내용 |
|---|---|
| [01-architecture.md](./01-architecture.md) | 전체 아키텍처: 기술 스택, 요청 처리 흐름, 인증/예외/응답 구조 |
| [02-layer-structure.md](./02-layer-structure.md) | 레이어 구성: Filter → Controller → Service → Repository → Entity, 도메인별 구현 현황 |
| [03-code-convention.md](./03-code-convention.md) | 코드 컨벤션: 네이밍, DTO/예외/설정 작성 패턴, 발견된 비일관성 및 기술부채 |
| [04-beginner-guide.md](./04-beginner-guide.md) | 비전공자를 위한 전반적인 흐름 설명 (로그인, 게시글 조회, 파일 업로드를 예시로) |

## 한 줄 요약

Meerkatgram 백엔드는 **Spring Boot 3.5 + Spring Security(Stateless JWT) + Spring Data JPA/QueryDSL** 기반의 REST API 서버이며, 도메인(auth/file/post/user)별로 Controller-Service-Repository 3계층 구조를 반복하는 전형적인 **레이어드 아키텍처**를 따릅니다.
