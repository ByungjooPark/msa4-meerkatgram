# doc/1st-doc 갱신 계획

> `00-current-state-survey.md`(코드 조사 결과)를 근거로 `doc/1st-doc/`의 7개 문서를
> 현재 코드(`feature/v2/migration-jpa`)에 맞게 수정하기 위한 작업 계획이다.
> 실제 문서 수정 전, 이 계획에 대한 승인을 받는다.

## 작업 원칙 (사용자 확인 완료)

- **덮어쓰기 vs 신규 파일**: 이번 작업은 "구 버전(MyBatis) 문서를 현재 코드에 맞게 고치는 것"이므로
  기존 7개 파일을 직접 수정한다. (스코프 축소가 아니라 사실관계 갱신이므로 별도 티어 문서를 만들 필요는 없음)
- **v2 예정 라벨**: `comments`/`likes`/`notifications`/`push_subscriptions`는 **이번 갱신 범위에 포함하지 않는다.**
  "v2 예정(미구현)" 라벨을 그대로 유지하고, ERD/스키마 오타(`NOMAL`, `nick` UK 등) 같은 이미 계획된 최소 정정만 반영한다.
- **게시글 작성(`POST /api/posts`) — 구현 완료 확인됨**: `PostController.store()` + `PostService.store()`가 실제로
  동작한다 (작업 트리에 미커밋 상태, `git status`상 `modified`). 흐름: `@Valid PostStoreReq`(content, image) +
  `@AuthenticationPrincipal Claims`로 인증된 `userId` 획득 → `new Post()` + setter로 필드 채움 →
  `user` 필드는 `userRepository.getReferenceById(userId)`(프록시, 추가 SELECT 없음) → `postRepository.save(post)` →
  `PostWithUserRes.from(...)`으로 응답. 문서의 "미구현" 표기를 전부 제거하고 실제 구현 스펙으로 교체한다.
  - `PostWithoutUserRes.java`(신규 미사용 파일)는 현재 어디에서도 참조되지 않는 죽은 코드다.
    `store()`는 `PostWithoutUserRes`가 아니라 `PostWithUserRes`를 반환한다. 문서에는 반영하지 않되,
    `03-backend-architecture.md`의 dead-code 각주 목록에 `DuplicatedUserException`과 함께 추가해 둔다.
- **게시글 삭제(`DELETE /api/posts/{id}`) — 여전히 미구현**: 컨트롤러에 해당 메서드 자체가 없다. 이 부분만
  기존 계획대로 "⚠️ 현재 미구현" 표기를 유지한다.
- **DB 스키마 원본 없음 → 임시 `ddl-auto`로 생성**: `meerkatgram-scheme.sql`은 현재 저장소에 없다. 신규 개발 환경
  세팅 시 `spring.jpa.hibernate.ddl-auto`를 일시적으로 `update`(또는 `create`)로 켜서 Hibernate가 엔티티 기준으로
  테이블을 자동 생성하게 한 뒤, 확인 후 다시 `none`으로 되돌리는 방식을 사용한다. `07-setup-guide.md`에 이 절차와
  "운영 환경에서는 절대 `update`/`create`를 켜지 말 것"이라는 주의문을 함께 기재한다.

---

## 파일별 변경 목록

### 01-project-overview.md
1. 기술 스택 표: MyBatis(3.0.5) 행 제거 → Spring Data JPA + QueryDSL(5.1.0 jakarta) 행 추가, springdoc-openapi 행 신규 추가, Spring Boot 버전 `3.5` → `3.5.15-SNAPSHOT`(스냅샷 주의 문구 포함) 갱신.
2. API 엔드포인트 목록:
   - `POST /api/users`(회원가입) → `POST /api/registration`으로 경로 변경.
   - `GET /api/users/{id}` 삭제(컨트롤러 자체가 빈 스텁, 존재하지 않음).
   - `POST /api/posts`(작성)는 구현 완료 — 표기 그대로 유지(인증 필요).
   - `DELETE /api/posts/{id}`(삭제)에만 "(미구현)" 표기 추가.
   - `/api/images/posts`, `/api/images/profiles` → `/api/files/posts`, `/api/files/profiles`로 경로 변경.
3. 요청 처리 흐름 다이어그램: `[Mapper Layer (MyBatis)]` → `[Repository Layer (Spring Data JPA + QueryDSL)]`로 교체.
4. 공통 응답 형식 예시: `"message": "정상 처리"` → `"message": "SUCCESS"`(실제 enum 상수명 그대로 반환됨을 반영).

### 02-erd-and-database.md
1. ERD 다이어그램: `nick` UK 제거(유니크 아님), `varchar_15` → `varchar_20`, `role`의 `NOMAL` 오타 → `NORMAL`, `posts.image`를 nullable → not-null로 수정.
2. `users` 테이블 스키마 표: 위와 동일하게 `nick` UNIQUE 제거·길이 변경, `role` 오타 수정, `created_at`/`updated_at` NULL 여부 `YES`→`NO`(`@CreatedDate`/`@LastModifiedDate`가 `nullable=false`).
3. `posts` 테이블 스키마 표: `image` NULL `YES`→`NO`, `user_id` FK 설명에 "물리적 FK 제약 없음(`NO_CONSTRAINT`), 수정 불가(`updatable=false`)" 각주 추가.
4. `comments`/`likes`/`notifications`/`push_subscriptions`: 기존 "v2 예정" 라벨 유지하되, 섹션 상단에 "아래 4개 테이블/엔티티는 현재 코드에 존재하지 않는 설계안이다"라는 문구를 명확히 추가.
5. FK 관계 표: `posts.user_id`는 개념적 FK일 뿐 실제 DB 제약이 없다는 점을 표에 각주로 추가.
6. **섹션 5 전면 교체**: "MyBatis ResultMap" 섹션(현재는 `<resultMap>`, `useGeneratedKeys` XML 예시) → "JPA 엔티티 매핑" 섹션으로 교체. `@SQLDelete`/`@SQLRestriction`(소프트 삭제 자동화), `@Column(name=...)`(snake_case ↔ camelCase 매핑), `@GeneratedValue(strategy = IDENTITY)`(PK 자동 채번) 코드 예시로 대체.
7. 섹션 4(소프트 삭제 패턴)의 XML 코드 블록(`PostMapper.xml`, `UserMapper.xml`)을 실제 `@SQLDelete`/`@SQLRestriction` 어노테이션 코드로 교체.

### 03-backend-architecture.md
1. 레이어 다이어그램: "Mapper Layer (MyBatis)" → "Repository Layer (Spring Data JPA + QueryDSL)".
2. 패키지 구조 트리 전면 갱신:
   - `mapper/` 디렉토리 전부 제거 → `repositories/`(각 도메인) 로 교체.
   - `auth`: `entities/`(빈 디렉토리) 존재 명시, `repositories/AuthRepository.java` 추가.
   - `post`: `repositories/PostRepository.java`, `PostQueryRepository.java` 추가.
   - `user`: `repositories/UserRepository.java`, `UserController`/`UserService`가 빈 스텁임을 각주로 명시.
   - `global/config/`에 `jpa/QueryDSLConfig.java`, `openapi/OpenApiConfig.java`·`CustomApiResponse.java`·`ApiResponseCustomizer.java` 추가.
   - `global/errors/custom/`: `FileStorageException` → `FileManagedException`으로 이름 교체, `DeletedRecordException`·`DuplicatedRecordException` 추가, 미사용 `DuplicatedUserException` 각주로 명시.
   - `security/jwt/JwtTokenProvider.java` → `JwtProvider.java`로 이름 교체.
   - `post/responses/`에 미사용(dead) `PostWithoutUserRes.java`가 있다는 점을 각주로 명시(실제 응답은 `PostWithUserRes` 사용).
   - 하단 `src/main/resources/mapper/...` 트리 전체 삭제(더 이상 존재하지 않음).
3. **섹션 4(회원가입 요청 흐름 예시) 전면 교체**: 현재 예시는 `POST /api/users` + `UserController.store()` + `userMapper` 기반인데, 실제로는 `POST /api/registration` + `AuthController` + `AuthService.registration()` + `AuthRepository`(JPA) 흐름이다. 실제 코드(`AuthService.java` 기준: `existsByEmail` 체크 → `DuplicatedRecordException` → `User` 엔티티 생성 → `authRepository.save()`)로 코드 스니펫을 다시 작성.
4. 섹션 5(`GlobalRes<T>`) 코드 예시 교체: 실제로는 `@Getter @Builder` 클래스가 아니라 **record**(`public record GlobalRes<T>(String code, String message, T data)`)이며, 정적 팩토리 메서드(`from`, `success`)를 사용한다. `.builder()...build()` 패턴 예시를 `GlobalRes.success(result)` 형태로 교체.
5. 섹션 6(에러 처리) 표 갱신: 예외 클래스명·코드 전체를 실제 `GlobalExceptionHandler` 매핑표(§8 in survey)로 교체 — `E20`/`E30` 삭제, `E02`/`E03`/`E10`/`E11`/`E40` 반영, `FileStorageException`→`FileManagedException`, `DeletedRecordException`/`DuplicatedRecordException` 추가.
6. 에러 응답 예시 JSON의 `message` 필드도 `"로그인 에러"` 같은 한글 문장 → `"NOT_REGISTERED_ERROR"` 같은 enum 상수명으로 교체.

### 04-api-specification.md
1. 공통 응답 형식 예시 전체: `"message": "정상 처리"` → `"message": "SUCCESS"`로 일괄 교체(모든 API 섹션).
2. 에러 코드 표(섹션 3) 전면 교체: 현재 9개 코드(E01~E99, E20/E30 포함) → 실제 11개 코드(SUCCESS, NOT_REGISTERED_ERROR, UNAUTHENTICATED_ERROR, UNAUTHORIZED_ERROR, INVALID_TOKEN_ERROR, NOT_FOUND_DATA_ERROR, DUPLICATED_DATA_ERROR, INVALID_PARAMETER_ERROR, FILE_MANAGED_ERROR, DB_ERROR, SYSTEM_ERROR)로 교체. 에러 응답 예시도 `data`가 문자열이 아니라 보통 `null`임을 반영(`MethodArgumentNotValidException`만 필드 오류를 로깅하고 `data`는 null).
3. **Auth API 섹션**: 회원가입(`POST /api/registration`)을 User 섹션에서 이쪽으로 이동. 실제 `RegistrationReq` 필드(`email`, `password`, `passwordChk`, `nick`, `profile`)와 검증 규칙(닉네임: 영문+숫자+언더바 2~20자, `@AssertTrue isPasswordMatch()`) 반영. 로그인 응답(`AuthRes`)도 `user` 필드가 `UserWithPostCountRes`(즉 `user` + `countPosts`)임을 반영해 예시 JSON 수정.
4. **User API 섹션 삭제**: `GET /api/users/{id}`, `POST /api/users`(회원가입) 둘 다 존재하지 않는 엔드포인트다. 섹션 전체를 제거하고, "UserController는 빈 스텁이며 현재 공개 엔드포인트가 없다. 유저 정보는 Auth/Post 응답에 중첩되어서만 노출된다"는 설명으로 대체.
5. **Post API 섹션**:
   - 6-1(목록): 응답 예시의 `posts[].userId` 필드를 실제 구조(`user: {id, email, nick, role, profile, createdAt}` 중첩 객체)로 교체. 필드명 오타 `createAt`(실제 코드 오타, `createdAt` 아님)을 그대로 문서화하고 "코드상 오타이며 그대로 응답됨" 각주 추가.
   - 6-2(상세): `GET /api/posts/{id}`가 인증 필요임을 유지(맞음), 응답 구조를 `PostWithUserRes` 실제 필드(`user` 중첩, `createAt` 오타 포함)로 교체.
   - 6-3(작성): 구현 완료. `PostStoreReq`(`content`, `image` 둘 다 필수) 실제 요청 스펙, `@AuthenticationPrincipal Claims`로 인증된 유저를 작성자로 저장하는 흐름 반영. 응답은 게시글 상세 조회와 동일한 `PostWithUserRes` 구조(작성자 정보 포함, `createAt` 오타 포함).
   - 6-4(삭제): "⚠️ 현재 미구현(컨트롤러에 메서드 자체가 없음)" 표기 유지. `SecurityUrlRegistry`에는 여전히 인증 필요로 등록되어 있으나 실제 라우트가 없다는 점도 각주로 남긴다. 예시 Request/Response는 "설계 참고용(구현 예정)"으로 표기하고 유지.
6. **File API 섹션**: 경로 `/api/images/posts`, `/api/images/profiles` → `/api/files/posts`, `/api/files/profiles`. 응답 필드 `fileUrl` → `fileUri`. 허용 확장자 목록에 `svg`, `gif` 추가(`FileConfig.allowExtensionList` 기준: jpg/jpeg/png/gif/svg/webp).

### 05-auth-jwt-guide.md
1. 클래스명 전역 교체: `JwtTokenProvider` → `JwtProvider`.
2. 메서드명 정정: `reissUri()` → `reissueUri()`.
3. `SecurityUrlRegistry` 코드 예시(섹션 5) 갱신: 실제 필드는 5개(GET/POST/PUT/PATCH/DELETE) 모두 존재하되 내용은 `AUTH_REQUIRED_GET_URLS = {"/api/posts/{id}"}`, `AUTH_REQUIRED_POST_URLS = {"/api/logout", "/api/posts"}`, `AUTH_REQUIRED_PUT_URLS = {}`, `AUTH_REQUIRED_PATCH_URLS = {}`(현재 비어있음, 구 문서의 `/api/users` PATCH 항목은 삭제), `AUTH_REQUIRED_DELETE_URLS = {"/api/posts/{id}"}`로 교체.
4. `AuthService.login`/`reissue` 코드 예시를 실제 코드(`authRepository.findByEmail(...).orElseThrow(...)`, `generateAuthentication` private 메서드가 `postRepository.countByUser`로 게시글 수까지 함께 조회) 기준으로 갱신.
5. `SecurityAuthenticationProvider` 클래스명은 실제 `SecurityAuthenticationProvider.java`와 일치하는지 재확인 후 필요시 필드/메서드 시그니처만 다듬는다 (survey상 큰 차이 없어 보임 — 코드 재확인 후 최소 수정).

### 06-key-features-guide.md
1. 섹션 1(`GlobalRes`/예외 처리): `03-backend-architecture.md`와 동일하게 record 기반 `GlobalRes` + 실제 예외 클래스 목록(`FileManagedException`, `DeletedRecordException`, `DuplicatedRecordException` 등)으로 교체.
2. 섹션 2(파일 업로드): `LocalFileManager` 코드 예시를 실제 메서드명으로 교체 — `getExtension` → `extractExtension`, 파일명 생성 로직은 `generateFileName()`(인자 없음, 확장자 미포함) + `generateProfilePath`/`generatePostPath`에서 조합하는 실제 구조로 수정. `WebConfig` 정적 리소스 매핑 경로 `/images/**` → `/files/**`(`fileConfig.storagePath() + "/files"`). `FileService.storePostImage()` 반환 필드 `fileUrl` → `fileUri`.
3. **섹션 3(게시글 CRUD) 대폭 수정**:
   - 3-1(목록/페이지네이션): `postMapper.getPagination()`/`getTotal()` → 실제 `PostQueryRepository.pagination(offset, limit)`(QueryDSL) + `postRepository.count()`(Spring Data JPA)로 교체.
   - 3-2(작성): 구현 완료. 실제 `PostService.store()` 코드(`new Post()` + setter, `userRepository.getReferenceById(userId)`로 프록시 참조만 걸어 불필요한 SELECT 없이 연관관계 설정, `postRepository.save(post)`)로 교체. `getReferenceById`가 실제 SELECT 없이 프록시만 반환한다는 점을 각주로 설명(교재 포인트로 유용).
   - 3-3(삭제): 컨트롤러/서비스에 해당 기능 자체가 없음(주석조차 없음, 아예 미구현)을 반영. "현재 미구현 — 필요 시 소프트 삭제(`postRepository.delete()`가 `@SQLDelete` 덕분에 자동으로 UPDATE로 치환됨) + 이미지 파일 정리 로직을 추가로 작성해야 한다"는 안내로 대체.
4. **섹션 4(MyBatis Mapper XML 작성 패턴) 전면 교체**: 더 이상 해당 사항 없음. "Spring Data JPA 리포지토리 작성 패턴"(`JpaRepository<Post, Long>` 상속 + 메서드 이름 기반 쿼리 `countByUser` 등)과 "QueryDSL 리포지토리 작성 패턴"(`PostQueryRepository`가 `@Repository` 클래스로 `JPAQueryFactory`를 직접 사용, `selectFrom(post).join(post.user, user).fetchJoin()...`)으로 교체.

### 07-setup-guide.md
1. 섹션 2(DB 생성 및 스키마 적용): `meerkatgram-scheme.sql` 파일이 더 이상 저장소에 없음. 절차를 아래와 같이 교체:
   1) DB(`meerkatgram`)만 생성.
   2) `application.yaml`의 `spring.jpa.hibernate.ddl-auto`를 **일시적으로** `update`(또는 `create`)로 설정하고 애플리케이션을 한 번 기동해 Hibernate가 엔티티 기준으로 테이블을 자동 생성하게 한다.
   3) 테이블 생성 확인 후 `ddl-auto`를 반드시 다시 `none`으로 되돌린다.
   4) "⚠️ `update`/`create`는 로컬 개발용 임시 조치이며, 운영 환경(`application-prod.yaml`)에서는 절대 사용하지 않는다"는 경고 문구 추가.
2. 섹션 3(`application.yaml` 설정 예시): `mybatis:` 블록 삭제. `security.jwt.reiss-uri` → `reissue-uri`. `springdoc:` 블록 신규 추가.
3. 섹션 5(curl 예시): 회원가입 엔드포인트 `/api/users` → `/api/registration`(Request Body에 `passwordChk` 필드 추가). 이미지 업로드 경로 `/api/images/...` → `/api/files/...`. 게시글 작성 curl 예시는 실제 구현된 스펙(`PostStoreReq`)으로 갱신.
4. 섹션 6(트러블슈팅): `FileStorageException` → `FileManagedException`으로 클래스명 교체.

---

## 진행 순서 제안

1. 위 계획에 대한 승인
2. 파일별로 순서대로 수정(01 → 02 → ... → 07), 각 파일 수정 후 변경 요약 보고
3. 전체 완료 후 `doc/1st-doc/` 전체를 훑어 파일 간 교차 참조(엔드포인트 경로, 에러 코드 등) 일관성 재검토
