# 04. API 명세서

## 1. 공통 요청 형식

| 항목 | 값 |
|------|----|
| Base URL | `http://localhost:8080` |
| Content-Type | `application/json` (파일 업로드는 `multipart/form-data`) |
| 인증 방식 | `Authorization: Bearer {accessToken}` |

---

## 2. 공통 응답 형식

모든 API는 아래 구조로 응답한다.

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": { ... }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `code` | String | 처리 결과 코드. `"00"` = 성공 |
| `message` | String | 처리 결과 메시지. `CustomResponseCode` enum 상수명이 그대로 들어간다(한글 문장 아님) |
| `data` | T | 응답 데이터. 없으면 `null` |

---

## 3. 에러 응답 형식 및 에러 코드 목록

에러 발생 시에도 `data`는 `null`이다. 상세 사유는 서버 로그로만 남고 응답에는 포함되지 않는다.

```json
{
  "code": "E01",
  "message": "NOT_REGISTERED_ERROR",
  "data": null
}
```

| 코드 | HTTP | enum 상수 | 발생 상황 |
|------|------|-----------|-----------|
| `00` | 200 | `SUCCESS` | 정상 처리 |
| `E01` | 401 | `NOT_REGISTERED_ERROR` | 이메일/비밀번호 불일치 |
| `E02` | 401 | `UNAUTHENTICATED_ERROR` | 인증 토큰 없이 인증 필요 API 호출 |
| `E03` | 403 | `UNAUTHORIZED_ERROR` | 접근 권한 없음 |
| `E04` | 401 | `INVALID_TOKEN_ERROR` | 토큰 형식 오류, 만료, 서명 위조 |
| `E10` | 404 | `NOT_FOUND_DATA_ERROR` | 조회 대상이 이미 삭제됨/존재하지 않음 |
| `E11` | 409 | `DUPLICATED_DATA_ERROR` | 이메일 등 중복 데이터 |
| `E21` | 400 | `INVALID_PARAMETER_ERROR` | 요청 파라미터 타입 오류 또는 `@Valid` 검증 실패 |
| `E40` | 500 | `FILE_MANAGED_ERROR` | 파일 저장/삭제 실패 |
| `E50` | 404 | `NOT_FOUND_ERROR` | 존재하지 않는 URL 요청 |
| `E80` | 500 | `DB_ERROR` | DB 에러 |
| `E99` | 500 | `SYSTEM_ERROR` | 알 수 없는 시스템 에러 |

---

## 4. Auth API

### 4-1. 로그인

```
POST /api/login
```

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "pass1234!"
}
```

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| `email` | String | O | 이메일 형식 |
| `password` | String | O | 영문·숫자·특수문자 8~20자 |

**Response `200`**

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": {
    "accessToken": "eyJhbGci...",
    "user": {
      "countPosts": 3,
      "user": {
        "id": 1,
        "email": "user@example.com",
        "nick": "meerkat",
        "role": "NORMAL",
        "profile": "http://localhost:8080/files/profiles/20250101_uuid.jpg",
        "createdAt": "2025-01-01T00:00:00"
      }
    }
  }
}
```

> `data.user`는 `UserWithPostCountRes` 구조로, 유저 정보(`user`)와 작성 게시글 수(`countPosts`)를 함께 담고 있다.
> Refresh Token은 `HttpOnly` 쿠키로 자동 설정된다. 클라이언트가 직접 다룰 필요 없음.

**에러 응답**

| 상황 | code | HTTP |
|------|------|------|
| 미가입 이메일 | `E01` | 401 |
| 비밀번호 불일치 | `E01` | 401 |

---

### 4-2. Access Token 재발급

```
POST /api/reissue-token
```

> 쿠키에 저장된 Refresh Token을 이용해 새 Access Token을 발급한다.
> 별도 Request Body 없음. 브라우저가 자동으로 쿠키를 전송한다.

**Response `200`** — 로그인 응답과 동일

**에러 응답**

| 상황 | code | HTTP |
|------|------|------|
| Refresh Token 쿠키 없음 | `E04` | 401 |
| DB의 Refresh Token과 불일치 | `E04` | 401 |
| 탈퇴 또는 존재하지 않는 유저 | `E04` | 401 |

---

### 4-3. 로그아웃 `🔒 인증 필요`

```
POST /api/logout
Authorization: Bearer {accessToken}
```

> Request Body 없음.
> DB의 `refresh_token`을 `NULL`로 초기화하고, 쿠키를 삭제한다.

**Response `200`**

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": null
}
```

---

### 4-4. 회원가입

```
POST /api/registration
```

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "pass1234!",
  "passwordChk": "pass1234!",
  "nick": "meerkat",
  "profile": "http://localhost:8080/files/profiles/20250101_uuid.jpg"
}
```

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| `email` | String | O | 이메일 형식 |
| `password` | String | O | 영문·숫자·특수문자 8~20자 |
| `passwordChk` | String | O | `password`와 값이 일치해야 함 (`@AssertTrue`로 교차검증) |
| `nick` | String | O | 영문·숫자·언더바 2~20자 (중복 불가는 아님) |
| `profile` | String | O | 프로필 이미지 URL (파일 업로드 후 획득) |

**Response `200`** — `data`는 `null` (별도 유저 정보를 반환하지 않는다)

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": null
}
```

**에러 응답**

| 상황 | code | HTTP |
|------|------|------|
| 이미 가입된 이메일 | `E11` | 409 |
| 필수 필드 누락 / 형식 오류 / 비밀번호 불일치 | `E21` | 400 |

---

## 5. User API — 공개 엔드포인트 없음

`UserController`는 필드/엔드포인트가 없는 빈 컨트롤러다. `GET /api/users/{id}`, `POST /api/users` 모두
존재하지 않는다(회원가입은 §4-4의 `POST /api/registration` 참고). 유저 정보는 항상 다른 API 응답에
중첩된 형태로만 노출된다 — 로그인/재발급 응답의 `data.user`(`UserWithPostCountRes`), 게시글 응답의
`data.user`(`UserRes`, §6-1·6-2 참고).

---

## 6. Post API

### 6-1. 게시글 목록 (페이지네이션)

```
GET /api/posts?page=1&limit=6
```

| 파라미터 | 위치 | 타입 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | Query | Integer | `1` | 페이지 번호 (1 이상) |
| `limit` | Query | Integer | `6` | 페이지당 게시글 수 (1 이상) |

**Response `200`**

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": {
    "total": 38,
    "lastPage": false,
    "posts": [
      {
        "id": 40,
        "content": "오늘의 한 컷",
        "image": "http://localhost:8080/files/posts/20250101_uuid.jpg",
        "createAt": "2025-01-10T12:00:00",
        "updatedAt": "2025-01-10T12:00:00",
        "deletedAt": null,
        "user": {
          "id": 3,
          "email": "user@example.com",
          "nick": "meerkat",
          "role": "NORMAL",
          "profile": "http://localhost:8080/files/profiles/20250101_uuid.jpg",
          "createdAt": "2025-01-01T00:00:00"
        }
      }
    ]
  }
}
```

> 작성자 정보는 `userId`가 아니라 `user` 객체(`UserRes`)로 중첩되어 내려온다.
> `createAt`은 `createdAt`이 아니라 **코드상 오타**이며, 고칠 계획이 없는 한 그대로 응답된다(`PostWithUserRes` 필드명 그대로).

| 필드 | 설명 |
|------|------|
| `total` | 전체 게시글 수 |
| `lastPage` | 마지막 페이지 여부 (`true`면 다음 페이지 없음) |
| `posts` | 게시글 배열 (최신순 정렬) |

---

### 6-2. 게시글 상세 조회 `🔒 인증 필요`

```
GET /api/posts/{id}
Authorization: Bearer {accessToken}
```

| 파라미터 | 위치 | 타입 | 제약 |
|----------|------|------|------|
| `id` | Path | Long | 1 이상 |

**Response `200`**

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": {
    "id": 1,
    "content": "오늘의 한 컷",
    "image": "http://localhost:8080/files/posts/20250101_uuid.jpg",
    "createAt": "2025-01-10T12:00:00",
    "updatedAt": "2025-01-10T12:00:00",
    "deletedAt": null,
    "user": {
      "id": 3,
      "email": "user@example.com",
      "nick": "meerkat",
      "role": "NORMAL",
      "profile": "http://localhost:8080/files/profiles/20250101_uuid.jpg",
      "createdAt": "2025-01-01T00:00:00"
    }
  }
}
```

> 목록 조회(6-1)와 동일하게 `user` 중첩 객체와 `createAt` 오타를 그대로 사용한다.

**에러 응답**

| 상황 | code | HTTP |
|------|------|------|
| 인증 토큰 없음 | `E02` | 401 |
| 토큰 오류/만료 | `E04` | 401 |
| `id` 타입 오류 (`/posts/abc`) | `E21` | 400 |

---

### 6-3. 게시글 작성 `🔒 인증 필요`

```
POST /api/posts
Authorization: Bearer {accessToken}
```

**Request Body**

```json
{
  "content": "오늘의 한 컷",
  "image": "http://localhost:8080/files/posts/20250101_uuid.jpg"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `content` | String | O | 게시글 내용 |
| `image` | String | O | 이미지 URL (파일 업로드 후 획득) |

**Response `200`** — 게시글 상세 조회(6-2)와 동일한 구조. 작성자(`user`)는 요청한 사용자(Access Token의 주체)로 자동 설정된다.

**에러 응답**

| 상황 | code | HTTP |
|------|------|------|
| 인증 토큰 없음 | `E02` | 401 |
| 토큰 오류/만료 | `E04` | 401 |
| 필수 필드 누락 | `E21` | 400 |

---

### 6-4. 게시글 삭제 `🔒 인증 필요` — ⚠️ 미구현

```
DELETE /api/posts/{id}
Authorization: Bearer {accessToken}
```

> **현재 컨트롤러에 해당 메서드 자체가 없다.** `SecurityUrlRegistry`에는 인증 필요 URL로 등록되어 있지만
> 실제 라우트가 없어 호출 시 404가 반환된다. 아래 스펙은 구현 예정 설계안이다.

| 파라미터 | 위치 | 타입 | 제약 |
|----------|------|------|------|
| `id` | Path | Long | 1 이상 |

**Response `200`** (설계안)

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": null
}
```

> 소프트 삭제 처리 (`deleted_at` 업데이트, `@SQLDelete`가 자동 처리). 연결된 이미지 파일도 서버에서 삭제하는 로직이 추가로 필요하다.

---

## 7. File API

### 7-1. 게시글 이미지 업로드

```
POST /api/files/posts
Content-Type: multipart/form-data
```

| 필드 | 타입 | 필수 | 허용 형식 |
|------|------|------|-----------|
| `file` | MultipartFile | O | jpg, jpeg, png, gif, svg, webp |

**Response `200`**

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": {
    "fileUri": "http://localhost:8080/files/posts/20250101_550e8400-e29b-41d4-a716-446655440000.jpg"
  }
}
```

> 반환된 `fileUri`를 게시글 작성 시 `image` 필드에 그대로 사용한다.

**에러 응답**

| 상황 | code | HTTP |
|------|------|------|
| 파일 없음/허용하지 않는 확장자/저장 실패 | `E40` | 500 |

---

### 7-2. 프로필 이미지 업로드

```
POST /api/files/profiles
Content-Type: multipart/form-data
```

| 필드 | 타입 | 필수 | 허용 형식 |
|------|------|------|-----------|
| `file` | MultipartFile | O | jpg, jpeg, png, gif, svg, webp |

**Response `200`**

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": {
    "fileUri": "http://localhost:8080/files/profiles/20250101_550e8400-e29b-41d4-a716-446655440000.png"
  }
}
```

> 반환된 `fileUri`를 회원가입 시 `profile` 필드에 그대로 사용한다.

**에러 응답**

| 상황 | code | HTTP |
|------|------|------|
| 파일 없음/허용하지 않는 확장자/저장 실패 | `E40` | 500 |
