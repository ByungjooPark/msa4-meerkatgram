# 07. 환경 설정 및 실행 가이드

## 1. 사전 설치 목록

| 도구 | 버전 | 확인 명령 |
|------|------|-----------|
| JDK | 17 이상 | `java -version` |
| MySQL | 8.4 | `mysql --version` |
| Gradle | 8.x (Wrapper 사용 가능) | `./gradlew --version` |
| IntelliJ IDEA | 최신 권장 | — |

> Gradle은 별도 설치 없이 프로젝트의 `gradlew` (Mac/Linux) 또는 `gradlew.bat` (Windows) Wrapper를 사용할 수 있다.

---

## 2. DB 생성 및 스키마 적용

### 2-1. DB 생성

```sql
CREATE DATABASE IF NOT EXISTS meerkatgram
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
```

### 2-2. 스키마 생성 — 엔티티 기반 자동 생성 (임시)

이 프로젝트에는 별도의 스키마 SQL 파일이 없다. `spring.jpa.hibernate.ddl-auto: none`이 기본값이라
Hibernate가 자동으로 테이블을 만들어주지도 않는다. 로컬 개발 환경에서는 아래 절차로 **일시적으로만**
`ddl-auto`를 켜서 엔티티(`User`, `Post`) 기준으로 테이블을 생성한다.

1. `application.yaml`의 `spring.jpa.hibernate.ddl-auto` 값을 `none`에서 `update`(또는 `create`)로 임시 변경한다.
2. 애플리케이션을 한 번 기동한다(`./gradlew bootRun`). Hibernate가 엔티티를 스캔해서 `users`, `posts` 테이블을 자동 생성한다.
3. DB 클라이언트(HeidiSQL/DBeaver 등)로 테이블이 정상 생성됐는지 확인한다.
4. **테이블 생성을 확인했으면 `ddl-auto` 값을 반드시 다시 `none`으로 되돌린다.**

> ⚠️ `update`/`create`는 로컬 개발 시 스키마를 처음 세팅할 때만 쓰는 임시 조치다.
> 운영 환경(`application-prod.yaml`)에서는 절대 `update`/`create`를 사용하지 않는다 — 의도치 않은 컬럼 변경/데이터 손실 위험이 있다.

**생성되는 테이블**

| 테이블 | v1 사용 여부 |
|--------|-------------|
| `users` | O |
| `posts` | O |
| `comments` | v2 예정 (미구현, 엔티티 없음) |
| `likes` | v2 예정 (미구현, 엔티티 없음) |
| `notifications` | v2 예정 (미구현, 엔티티 없음) |
| `push_subscriptions` | v2 예정 (미구현, 엔티티 없음) |

---

## 3. `application.yaml` 설정

> `application.yaml`(dev/default), `application-prod.yaml`(prod)은 개발용 기본값을 포함한 채 **git에 커밋되어 있다**.
> 대부분의 값은 `${DB_HOST:localhost}`처럼 환경변수 오버라이드 + 기본값 형태이므로, 로컬 개발 시에는 별도 생성 없이도
> 바로 실행 가능하다. `security.jwt.secret`처럼 민감한 값을 바꾸고 싶다면 아래 구조를 참고해 직접 값을 채우거나
> 환경변수로 오버라이드한다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://{호스트}:{포트}/{데이터베이스명}?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: {DB 사용자명}
    password: {DB 비밀번호}
    driver-class-name: com.mysql.cj.jdbc.Driver

  servlet:
    multipart:
      max-file-size: 10MB      # 단일 파일 최대 크기
      max-request-size: 20MB   # 요청 전체 최대 크기

  jpa:
    hibernate:
      ddl-auto: none            # 스키마는 자동 생성하지 않음 (§2-2의 임시 절차 참고)
    show-sql: true
    format_sql: true

springdoc:
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: alpha
  api-docs:
    path: /api-docs

security:
  jwt:
    secure: false                       # 로컬 개발: false / 운영(HTTPS): true
    issuer: meerkatgram
    type: JWT
    access-token-expiry: 3600000        # Access Token 만료: 1시간 (ms)
    refresh-token-expiry: 604800000     # Refresh Token 만료: 7일 (ms)
    refresh-token-cookie-name: refresh_token
    refresh-token-cookie-expiry: 604800 # 쿠키 만료: 7일 (초)
    secret: {Base64로 인코딩된 256비트 이상 비밀키}
    header-key: Authorization
    scheme: Bearer
    reissue-uri: /api/reissue-token

file:
  storage-path: {파일 저장 루트 절대 경로}   # 예: /app/storage 또는 C:/meerkatgram/storage
  profile-path: /files/profiles
  post-path: /files/posts
  server-uri: http://localhost:8080
  allow-extension-list:
    - image/jpg
    - image/jpeg
    - image/png
    - image/gif
    - image/svg
    - image/webp

cors:
  allowed-origins:
    - http://localhost:5173              # Vue 개발 서버 주소
  max-age: 3600
```

### 주요 설정 항목 설명

| 항목 | 설명 |
|------|------|
| `security.jwt.secret` | JWT 서명에 사용하는 비밀 키. Base64 인코딩된 256비트(32바이트) 이상 문자열 |
| `security.jwt.secure` | Refresh Token 쿠키의 `Secure` 속성. 로컬은 `false`, HTTPS 운영 환경은 `true` |
| `file.storage-path` | 업로드 파일의 실제 저장 위치. 절대 경로 사용 권장 |
| `file.server-uri` | 클라이언트에 반환할 파일 URL의 Base URL |
| `cors.allowed-origins` | CORS 허용 출처 목록. 프론트엔드 주소를 정확히 입력 |

**Base64 비밀 키 생성 방법**

```bash
# Mac/Linux
openssl rand -base64 32

# Java 코드로 생성
import java.util.Base64;
import java.security.SecureRandom;

byte[] key = new byte[32];
new SecureRandom().nextBytes(key);
System.out.println(Base64.getEncoder().encodeToString(key));
```

---

## 4. 백엔드 빌드 및 실행

### 4-1. 의존성 설치 및 빌드

```bash
# 프로젝트 루트에서 실행
./gradlew build          # Mac/Linux
gradlew.bat build        # Windows
```

### 4-2. 실행

```bash
./gradlew bootRun        # 개발 모드 실행
```

또는 IntelliJ에서 `Msa4MeerkatgramApplication.java`의 `main` 메서드 옆 실행 버튼을 클릭한다.

### 4-3. 정상 실행 확인

브라우저 또는 curl로 아래 요청을 보냈을 때 응답이 오면 정상이다.

```bash
curl http://localhost:8080/api/posts
```

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": { "total": 0, "lastPage": true, "posts": [] }
}
```

---

## 5. API 테스트 — curl 예시 (Postman으로 대체 가능)

### 회원가입 전 프로필 이미지 업로드

```bash
curl -X POST http://localhost:8080/api/files/profiles \
  -F "file=@/path/to/image.jpg"
```

### 회원가입

```bash
curl -X POST http://localhost:8080/api/registration \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "pass1234!",
    "passwordChk": "pass1234!",
    "nick": "meerkat",
    "profile": "http://localhost:8080/files/profiles/20250101_uuid.jpg"
  }'
```

### 로그인 (쿠키 저장 포함)

```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "email": "test@example.com",
    "password": "pass1234!"
  }'
```

> `-c cookies.txt`: 응답의 Set-Cookie를 파일에 저장 (Refresh Token 보관)

### 게시글 작성 (Access Token 필요)

```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {로그인 응답의 accessToken}" \
  -d '{
    "content": "오늘의 한 컷",
    "image": "http://localhost:8080/files/posts/20250101_uuid.jpg"
  }'
```

### Access Token 재발급 (쿠키 사용)

```bash
curl -X POST http://localhost:8080/api/reissue-token \
  -b cookies.txt
```

> `-b cookies.txt`: 저장된 쿠키 파일을 요청에 첨부

---

## 6. 트러블슈팅

### `java.sql.SQLException: Access denied for user`

- `application.yaml`의 `spring.datasource.username`, `password` 확인
- MySQL 사용자에게 `meerkatgram` DB 권한 부여 확인

```sql
GRANT ALL PRIVILEGES ON meerkatgram.* TO '{username}'@'localhost';
FLUSH PRIVILEGES;
```

---

### `io.jsonwebtoken.security.WeakKeyException`

- `security.jwt.secret` 값이 너무 짧은 경우 발생
- Base64로 인코딩된 256비트(32바이트) 이상의 키를 사용해야 한다

---

### CORS 에러 (브라우저 콘솔)

```
Access to fetch at 'http://localhost:8080/...' from origin 'http://localhost:5173' has been blocked by CORS policy
```

- `cors.allowed-origins` 목록에 프론트엔드 주소가 정확히 포함되어 있는지 확인
- 포트 번호, 프로토콜(`http`/`https`) 포함 정확히 일치해야 한다

---

### 파일 업로드 실패 (`FileManagedException`)

- `file.storage-path`가 서버에 실제로 존재하는 경로인지 확인
- 해당 경로에 쓰기 권한이 있는지 확인

```bash
# Mac/Linux — 쓰기 권한 부여
chmod -R 755 /path/to/storage
```

---

### `E21 — 요청 파라미터에 이상이 있습니다`

- Request Body의 필드명, 타입, 필수 여부 확인
- `Content-Type: application/json` 헤더 누락 여부 확인
- Path Variable이 숫자가 아닌 경우 (`/posts/abc` → `/posts/1` 사용)
