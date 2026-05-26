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
  "message": "정상 처리",
  "data": { ... }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `code` | String | 처리 결과 코드. `"00"` = 성공 |
| `message` | String | 처리 결과 메시지 |
| `data` | T | 응답 데이터. 없으면 `null` |

---

## 3. 에러 응답 형식 및 에러 코드 목록

에러 발생 시 `data`에 상세 메시지를 포함한다.

```json
{
  "code": "E01",
  "message": "로그인 에러",
  "data": "아이디와 비밀번호를 확인해주세요."
}
```

| 코드 | HTTP | 발생 상황 |
|------|------|-----------|
| `E01` | 401 | 이메일/비밀번호 불일치 (`NotRegisteredException`) |
| `E02` | 401 | 인증 토큰 없이 인증 필요 API 호출 |
| `E03` | 403 | 접근 권한 없음 |
| `E04` | 400 | 토큰 형식 오류, 만료, 서명 위조 (`InvalidTokenException`) |
| `E20` | 404 | 존재하지 않는 URL |
| `E21` | 400 | 요청 파라미터 타입 오류 또는 `@Valid` 검증 실패 |
| `E30` | 400 | 파일 저장 실패 또는 기타 런타임 에러 |
| `E80` | 500 | DB 에러 |
| `E99` | 500 | 알 수 없는 시스템 에러 |

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
  "message": "정상 처리",
  "data": {
    "accessToken": "eyJhbGci...",
    "user": {
      "id": 1,
      "email": "user@example.com",
      "nick": "meerkat",
      "role": "NORMAL",
      "profile": "http://localhost:8080/images/profiles/20250101_uuid.jpg",
      "createdAt": "2025-01-01T00:00:00"
    }
  }
}
```

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
| Refresh Token 쿠키 없음 | `E04` | 400 |
| DB의 Refresh Token과 불일치 | `E04` | 400 |
| 탈퇴 또는 존재하지 않는 유저 | `E04` | 400 |

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
  "message": "정상 처리",
  "data": null
}
```

---

## 5. User API

### 5-1. 유저 정보 조회

```
GET /api/users/{id}
```

| 파라미터 | 위치 | 타입 | 제약 |
|----------|------|------|------|
| `id` | Path | Long | 1 이상 |

**Response `200`**

```json
{
  "code": "00",
  "message": "정상 처리",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "nick": "meerkat",
    "role": "NORMAL",
    "profile": "http://localhost:8080/images/profiles/20250101_uuid.jpg",
    "createdAt": "2025-01-01T00:00:00"
  }
}
```

---

### 5-2. 회원가입

```
POST /api/users
```

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "pass1234!",
  "nick": "meerkat",
  "profile": "http://localhost:8080/images/profiles/20250101_uuid.jpg"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | String | O | 이메일 형식, 중복 불가 |
| `password` | String | O | 영문·숫자·특수문자 8~20자 |
| `nick` | String | O | 닉네임, 중복 불가 |
| `profile` | String | O | 프로필 이미지 URL (파일 업로드 후 획득) |

**Response `200`** — 유저 정보 조회 응답과 동일

**에러 응답**

| 상황 | code | HTTP |
|------|------|------|
| 이미 가입된 이메일 | `E30` | 400 |
| 필수 필드 누락 / 형식 오류 | `E21` | 400 |

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
  "message": "정상처리",
  "data": {
    "total": 38,
    "lastPage": false,
    "posts": [
      {
        "id": 40,
        "userId": 3,
        "content": "오늘의 한 컷",
        "image": "http://localhost:8080/images/posts/20250101_uuid.jpg",
        "createdAt": "2025-01-10T12:00:00",
        "updatedAt": "2025-01-10T12:00:00",
        "deletedAt": null
      }
    ]
  }
}
```

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
  "message": "정상 처리",
  "data": {
    "id": 1,
    "userId": 3,
    "content": "오늘의 한 컷",
    "image": "http://localhost:8080/images/posts/20250101_uuid.jpg",
    "createdAt": "2025-01-10T12:00:00",
    "updatedAt": "2025-01-10T12:00:00",
    "deletedAt": null
  }
}
```

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
  "image": "http://localhost:8080/images/posts/20250101_uuid.jpg"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `content` | String | O | 게시글 내용 (200자 이내) |
| `image` | String | O | 이미지 URL (파일 업로드 후 획득) |

**Response `200`** — 게시글 상세 조회 응답과 동일

---

### 6-4. 게시글 삭제 `🔒 인증 필요`

```
DELETE /api/posts/{id}
Authorization: Bearer {accessToken}
```

| 파라미터 | 위치 | 타입 | 제약 |
|----------|------|------|------|
| `id` | Path | Long | 1 이상 |

**Response `200`**

```json
{
  "code": "00",
  "message": "정상 처리",
  "data": null
}
```

> 소프트 삭제 처리 (`deleted_at` 업데이트). 연결된 이미지 파일도 서버에서 삭제된다.

---

## 7. File API

### 7-1. 게시글 이미지 업로드

```
POST /api/images/posts
Content-Type: multipart/form-data
```

| 필드 | 타입 | 필수 | 허용 형식 |
|------|------|------|-----------|
| `file` | MultipartFile | O | jpg, jpeg, png, webp |

**Response `200`**

```json
{
  "code": "00",
  "message": "정상 처리",
  "data": {
    "fileUrl": "http://localhost:8080/images/posts/20250101_550e8400-e29b-41d4-a716-446655440000.jpg"
  }
}
```

> 반환된 `fileUrl`을 게시글 작성 시 `image` 필드에 그대로 사용한다.

---

### 7-2. 프로필 이미지 업로드

```
POST /api/images/profiles
Content-Type: multipart/form-data
```

| 필드 | 타입 | 필수 | 허용 형식 |
|------|------|------|-----------|
| `file` | MultipartFile | O | jpg, jpeg, png, webp |

**Response `200`**

```json
{
  "code": "00",
  "message": "정상 처리",
  "data": {
    "fileUrl": "http://localhost:8080/images/profiles/20250101_550e8400-e29b-41d4-a716-446655440000.png"
  }
}
```

> 반환된 `fileUrl`을 회원가입 시 `profile` 필드에 그대로 사용한다.
