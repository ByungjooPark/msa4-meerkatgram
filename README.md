# Meerkatgram

사용자가 이미지 게시글을 올리고 소통하는 **커뮤니티형 웹 애플리케이션**입니다.  
백엔드(Spring Boot 3)와 프론트엔드(Vue 3)는 분리된 프로젝트로 관리하며 HTTP API로 통신합니다.

---

## 화면 디자인

![Design](https://github.com/user-attachments/assets/36813f6a-a155-4ffe-a341-8f4413fa4520)

---

## ERD

![ERD](https://github.com/user-attachments/assets/4ae34858-7936-415a-9081-d7b3435343d3)

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| Language | Java 17 |
| Backend | Spring Boot 3.5, Spring Security, MyBatis 3.0.5 |
| Database | MySQL 8.4 |
| Auth | JWT (Access / Refresh Token) |
| Frontend | Vue 3 (Composition API), Pinia, Vue Router 4, Axios, Vite |
| Build | Gradle |

---

## 주요 기능

| 분류 | 기능 |
|------|------|
| 인증 | 회원가입, 로그인, 로그아웃, Access Token 재발급 |
| 유저 | 유저 정보 조회 |
| 게시글 | 목록(페이지네이션), 상세 조회, 작성, 삭제 |
| 파일 | 게시글 이미지 업로드, 프로필 이미지 업로드 |

---

## 상세 문서

| 문서 | 설명 |
|------|------|
| [프로젝트 개요](./meerkatgram-doc/1st-doc/01-project-overview.md) | 기능 목록, 기술 스택, 요청 처리 흐름 |
| [ERD & 데이터베이스](./meerkatgram-doc/1st-doc/02-erd-and-database.md) | 테이블 스키마, 관계, 소프트 삭제 패턴 |
| [백엔드 아키텍처](./meerkatgram-doc/1st-doc/03-backend-architecture.md) | 레이어 구조, 패키지 설계 |
| [API 명세](./meerkatgram-doc/1st-doc/04-api-specification.md) | 전체 엔드포인트, 요청/응답 예시 |
| [JWT 인증 가이드](./meerkatgram-doc/1st-doc/05-auth-jwt-guide.md) | 토큰 발급·갱신·무효화 흐름 |
| [주요 기능 가이드](./meerkatgram-doc/1st-doc/06-key-features-guide.md) | 게시글, 파일 업로드 등 구현 상세 |
| [개발 환경 세팅](./meerkatgram-doc/1st-doc/07-setup-guide.md) | 로컬 실행 방법 |
