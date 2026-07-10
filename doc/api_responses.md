# Meerkatgram API 응답 및 에러 코드 명세서

이 문서는 Meerkatgram 백엔드 애플리케이션의 각 API별 성공 응답 및 발생 가능한 에러(HttpStatus 및 에러 코드)의 대응 관계를 정리한 명세서입니다.

---

## 1. 공통 응답 포맷 (`GlobalRes`)

모든 API 응답은 일관된 공통 응답 포맷(`GlobalRes`)으로 감싸져 반환됩니다.

```json
{
  "code": "응답 코드 (성공: '00', 에러: 'E**')",
  "message": "결과 메시지",
  "data": "응답 데이터 (성공 시 반환 데이터 객체, 에러 시 구체적인 오류 정보)"
}
```

---

## 2. 공통 에러 코드 명세 (소분류)

스프링 예외 처리 핸들러([GlobalExceptionHandler.java](file:///E:/security/meerkat-msa4/workspace/msa4-meerkatgram/src/main/java/com/msa4meerkatgram/global/errors/GlobalExceptionHandler.java)) 및 보안 예외 처리기([SecurityExceptionHandler.java](file:///E:/security/meerkat-msa4/workspace/msa4-meerkatgram/src/main/java/com/msa4meerkatgram/global/security/filter/SecurityExceptionHandler.java))에서 정의 및 처리되는 공통 에러 코드 목록입니다.

| 에러 코드 | HTTP Status | 에러 메시지 (message) | 발생 원인 / 비고 | 대응 예외 클래스 |
| :--- | :--- | :--- | :--- | :--- |
| **E01** | `401 Unauthorized` | 로그인 에러 | 로그인 시 아이디 혹은 비밀번호가 일치하지 않음 | `NotRegisteredException` |
| **E02** | `401 Unauthorized` | UNAUTHENTICATED_ERROR | 인증 헤더가 누락되었거나 인증되지 않은 접근 | `AuthenticationException` |
| **E03** | `403 Forbidden` | UNAUTHORIZED_ERROR | 접근에 필요한 인가(권한)가 부족함 | `AccessDeniedException` |
| **E04** | `401 Unauthorized` | 토큰 이상 | Access/Refresh Token이 유효하지 않거나 만료됨 | `InvalidTokenException` |
| **E10** | `404 Not Found` | DELETED_RECORD_ERROR | 조회하려는 데이터 레코드가 삭제되었거나 존재하지 않음 | `DeletedRecordException` |
| **E11** | `409 Conflict` | DUPLICATED_RECORD_ERROR | 등록하려는 데이터가 데이터베이스에 이미 중복 존재함 | `DuplicatedRecordException` |
| **E21** | `400 Bad Request` | 요청 파라미터에 이상이 있습니다. | API 요청 바디(DTO) 또는 파라미터 유효성 검증 실패 | `MethodArgumentNotValidException`<br>`MethodArgumentTypeMismatchException` |
| **E40** | `500 Internal Error` | 파일 업로드 실패 | 파일이 비어있음, 지원하지 않는 확장자, 디스크 쓰기 오류 등 | `FileManagedException` |
| **E80** | `500 Internal Error` | DB 에러 | 데이터베이스 커넥션 장애 또는 SQL 구문 예외 발생 | `SQLException` |
| **E99** | `500 Internal Error` | 시스템 에러 | 서버 애플리케이션 내부에서 처리되지 않은 모든 예외 | `Exception` (기타 모든 예외) |

---

## 3. API별 Response 상세 정의 (대분류 > 중분류 > 소분류)

### 🔑 인증 API (`AuthController`)

#### 1. 로그인 (`POST /api/login`)
- **`200 OK` (성공)**
  - **코드**: `00`
  - **메시지**: `로그인 완료`
  - **데이터**: `AuthRes` (로그인 유저 정보, 발급된 AccessToken 및 작성한 게시글 수)
- **`400 Bad Request`**
  - **코드**: `E21` (요청 파라미터 이상)
  - **데이터**: 필드별 유효성 에러 맵 (`Map<String, String>`)
- **`401 Unauthorized`**
  - **코드**: `E01` (로그인 에러)
  - **데이터**: `아이디와 비밀번호를 확인해주세요.`
- **`500 Internal Server Error`**
  - **코드**: `E80` (DB 에러) 또는 `E99` (시스템 에러)

#### 2. 토큰 재발급 (`POST /api/reissue-token`)
- **`200 OK` (성공)**
  - **코드**: `00`
  - **메시지**: `토큰 재발급 완료`
  - **데이터**: `AuthRes` (유저 정보, 재발급된 AccessToken 등)
- **`401 Unauthorized`**
  - **코드**: `E04` (토큰 이상)
  - **데이터**: `토큰이 없습니다.` / `유효하지 않은 회원의 토큰입니다.` / `토큰이 일치하지 않습니다.` 등 구체적인 원인
- **`500 Internal Server Error`**
  - **코드**: `E80` (DB 에러) 또는 `E99` (시스템 에러)

#### 3. 로그아웃 (`POST /api/logout`)
- **`200 OK` (성공)**
  - **코드**: `00`
  - **메시지**: `로그아웃 완료`
- **`401 Unauthorized`**
  - **코드**: `E02` (미인증)
    - **원인**: 로그인 인증(AccessToken 헤더) 정보가 유효하지 않거나 없을 때 필터 레벨에서 차단
  - **코드**: `E04` (토큰 이상)
    - **데이터**: `유효하지 않은 회원의 토큰입니다.`
- **`500 Internal Server Error`**
  - **코드**: `E80` (DB 에러) 또는 `E99` (시스템 에러)

#### 4. 회원가입 (`POST /api/registration`)
- **`200 OK` (성공)**
  - **코드**: `00`
  - **메시지**: `회원가입 완료`
- **`400 Bad Request`**
  - **코드**: `E21` (요청 파라미터 이상)
  - **데이터**: 필드별 유효성 에러 맵 (`Map<String, String>`)
- **`409 Conflict`**
  - **코드**: `E11` (중복 레코드 에러)
  - **데이터**: `이미 가입된 회원입니다.`
- **`500 Internal Server Error`**
  - **코드**: `E80` (DB 에러) 또는 `E99` (시스템 에러)

---

### 📂 파일 API (`FileController`)

#### 1. 프로필 이미지 업로드 (`POST /api/files/profiles`)
- **`200 OK` (성공)**
  - **코드**: `00`
  - **메시지**: `파일 저장 성공`
  - **데이터**: `FileRes` (저장 완료된 파일의 서버 내 전체 URI 주소)
- **`500 Internal Server Error`**
  - **코드**: `E40` (파일 업로드 실패)
    - **데이터**: `파일 저장 실패: 파일 확장자 획득 실패(파일 없음)` / `파일 저장 실패: 허용하지 않는 파일 확장자` / 디렉토리 생성 실패 등 구체적 상세 에러 메시지
  - **코드**: `E99` (시스템 에러)

#### 2. 게시글 이미지 업로드 (`POST /api/files/posts`)
- **`200 OK` (성공)**
  - **코드**: `00`
  - **메시지**: `파일 저장 성공`
  - **데이터**: `FileRes` (저장 완료된 파일의 서버 내 전체 URI 주소)
- **`500 Internal Server Error`**
  - **코드**: `E40` (파일 업로드 실패)
    - **데이터**: 이미지 업로드 실패 사유 상세 메시지
  - **코드**: `E99` (시스템 에러)

---

### 📝 게시글 API (`PostController`)

#### 1. 게시글 목록 조회 (페이징) (`GET /api/posts`)
- **`200 OK` (성공)**
  - **코드**: `00`
  - **메시지**: `정상처리`
  - **데이터**: `PostIndexRes` (전체 게시글 수, 마지막 페이지 여부, 현재 페이지의 게시글 목록)
- **`400 Bad Request`**
  - **코드**: `E21` (요청 파라미터 이상)
    - **원인**: 쿼리 스트링의 `page`나 `limit` 파라미터의 타입이 불일치하는 경우
- **`500 Internal Server Error`**
  - **코드**: `E80` (DB 에러) 또는 `E99` (시스템 에러)

#### 2. 게시글 상세 조회 (`GET /api/posts/{id}`)
> [!NOTE]
> `SecurityUrlRegistry` 설정에 의해 이 API 호출에는 **로그인 인증(Bearer AccessToken 헤더)**이 필수적으로 요구됩니다.
- **`200 OK` (성공)**
  - **코드**: `00`
  - **메시지**: `게시글 상세 정상 처리`
  - **데이터**: `PostWithUserRes` (작성자의 프로필/닉네임 정보가 포함된 게시글 데이터)
- **`400 Bad Request`**
  - **코드**: `E21` (요청 파라미터 이상)
    - **원인**: 경로 변수 `{id}` 값이 숫자가 아니거나 올바르지 않은 타입인 경우
- **`401 Unauthorized`**
  - **코드**: `E02` (미인증)
    - **원인**: 인증 헤더가 전송되지 않았거나 빈 경우
  - **코드**: `E04` (토큰 이상)
    - **원인**: 제공된 AccessToken이 만료되었거나 서명이 위조됨
- **`404 Not Found`**
  - **코드**: `E10` (이미 삭제된 레코드)
    - **데이터**: `이미 삭제된 게시글입니다.`
- **`500 Internal Server Error`**
  - **코드**: `E80` (DB 에러) 또는 `E99` (시스템 에러)
