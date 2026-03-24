# Skin Recipe — 테스트 가이드

## 테스트 실행 방법

```bash
# 전체 테스트 실행
./gradlew test

# 특정 클래스만 실행
./gradlew test --tests "com.mycosmetic.security.JwtUtilTest"
./gradlew test --tests "com.mycosmetic.service.AuthServiceTest"
./gradlew test --tests "com.mycosmetic.controller.AuthControllerTest"
./gradlew test --tests "com.mycosmetic.service.CosmeticServiceTest"
./gradlew test --tests "com.mycosmetic.controller.CosmeticControllerTest"
./gradlew test --tests "com.mycosmetic.service.OcrServiceTest"
./gradlew test --tests "com.mycosmetic.service.FileStorageServiceTest"

# 캐시 무시하고 강제 재실행
./gradlew test --rerun

# HTML 리포트 확인 (브라우저)
open build/reports/tests/test/index.html
```

---

## 테스트 클래스 목록

### 단위 테스트 (Spring 컨텍스트 없음)

---

#### `JwtUtilTest`
**위치:** `src/test/java/com/mycosmetic/security/JwtUtilTest.java`
**목적:** JWT 토큰 생성 / 검증 / 파싱 로직 확인

| 테스트명 | 검증 내용 |
|----------|-----------|
| 토큰 생성 후 이메일 추출이 일치해야 한다 | `generateToken(email)` → `getEmailFromToken()` 결과가 원본 이메일과 동일한지 |
| 유효한 토큰은 검증을 통과해야 한다 | 정상 발급된 토큰이 `validateToken()` = `true` 반환하는지 |
| 위변조된 토큰은 검증에 실패해야 한다 | 토큰 뒤에 임의 문자열 추가 시 `validateToken()` = `false` 반환하는지 |
| 만료된 토큰은 검증에 실패해야 한다 | `expiration = -1`로 생성한 토큰이 `validateToken()` = `false` 반환하는지 |

---

#### `AuthServiceTest`
**위치:** `src/test/java/com/mycosmetic/service/AuthServiceTest.java`
**Mock:** `UserRepository`, `BCryptPasswordEncoder`, `JwtUtil`
**목적:** 회원가입 / 로그인 비즈니스 로직 확인

| 테스트명 | 검증 내용 |
|----------|-----------|
| 정상적인 회원가입 요청이면 User가 저장된다 | `userRepository.save()` 1회 호출 확인 |
| 이미 존재하는 이메일이면 예외가 발생한다 | `existsByEmail = true` 일 때 `IllegalArgumentException` 발생 + `save()` 미호출 확인 |
| 올바른 이메일/비밀번호이면 JWT가 반환된다 | 로그인 성공 시 `accessToken` 포함된 `LoginResponse` 반환 확인 |
| 존재하지 않는 이메일이면 예외가 발생한다 | `findByEmail = empty` 일 때 `IllegalArgumentException` 발생 확인 |
| 비밀번호가 틀리면 예외가 발생한다 | `passwordEncoder.matches = false` 일 때 `IllegalArgumentException` 발생 확인 |

---

#### `CosmeticServiceTest`
**위치:** `src/test/java/com/mycosmetic/service/CosmeticServiceTest.java`
**Mock:** `CosmeticRepository`, `UserRepository`
**목적:** 화장품 CRUD 비즈니스 로직 및 소유자 검증 확인

| 테스트명 | 검증 내용 |
|----------|-----------|
| 내 화장품 목록을 조회한다 | `findAllByUserId()` 결과가 `CosmeticResponse` 리스트로 반환되는지 |
| 화장품을 정상 저장한다 | `cosmeticRepository.save()` 1회 호출 확인 |
| 내 화장품을 수정한다 | `update()` 호출 후 변경된 필드가 반영되는지 |
| 다른 사람의 화장품을 수정하면 예외가 발생한다 | 소유자 불일치 시 `IllegalArgumentException` 발생 확인 |
| 내 화장품을 삭제한다 | `cosmeticRepository.delete()` 1회 호출 확인 |
| 다른 사람의 화장품을 삭제하면 예외가 발생한다 | 소유자 불일치 시 `IllegalArgumentException` + `delete()` 미호출 확인 |

---

#### `OcrServiceTest`
**위치:** `src/test/java/com/mycosmetic/service/OcrServiceTest.java`
**Mock:** `UpstageOcrClient`, `UpstageLlmClient`, `FileStorageService`, `CosmeticService` (실제 외부 API 미호출)
**목적:** confidence 값에 따른 자동저장 / HITL 분기 로직 확인

| 테스트명 | 검증 내용 |
|----------|-----------|
| confidence가 high이면 자동 저장하고 CosmeticResponse를 반환한다 | `cosmeticService.save()` 호출 확인 + 반환 타입이 `CosmeticResponse`인지 |
| confidence가 low이면 저장하지 않고 OcrParseResult를 반환한다 | `cosmeticService.save()` 미호출 확인 + 반환 타입이 `OcrParseResult`인지 |

---

#### `FileStorageServiceTest`
**위치:** `src/test/java/com/mycosmetic/service/FileStorageServiceTest.java`
**비고:** `@TempDir`로 임시 디렉토리 사용 — 실제 `/uploads/` 미사용
**목적:** 파일 저장 경로 생성 및 실제 디스크 저장 동작 확인

| 테스트명 | 검증 내용 |
|----------|-----------|
| 파일을 저장하면 /uploads/ 경로를 반환한다 | 반환된 경로가 `/uploads/`로 시작하고 `.jpg`로 끝나는지 |
| 파일명은 UUID 기반으로 생성되어 중복되지 않는다 | 동일 파일 2회 저장 시 서로 다른 경로 반환하는지 |
| 저장된 파일이 실제로 디스크에 존재한다 | 반환된 경로에 해당하는 파일이 실제로 생성됐는지 |

---

### 슬라이스 테스트 (`@WebMvcTest` — HTTP 계층만 로드)

---

#### `AuthControllerTest`
**위치:** `src/test/java/com/mycosmetic/controller/AuthControllerTest.java`
**Mock:** `AuthService`, `JwtUtil`, `UserDetailsServiceImpl`
**목적:** 회원가입 / 로그인 HTTP 요청/응답, 상태 코드, Validation 확인

| 테스트명 | 검증 내용 |
|----------|-----------|
| 유효한 회원가입 요청이면 200과 메시지를 반환한다 | 정상 요청 시 `200 OK` + `{ "message": "회원가입이 완료되었습니다." }` 반환 확인 |
| 이메일 형식이 잘못되면 400을 반환한다 | `email: "not-an-email"` 전송 시 `@Email` Validation → `400` 반환 확인 |
| 중복 이메일이면 400을 반환한다 | `AuthService`가 `IllegalArgumentException` 던질 때 `GlobalExceptionHandler`가 `400` 변환하는지 확인 |
| 올바른 로그인 요청이면 200과 accessToken을 반환한다 | 정상 로그인 시 `200 OK` + `{ "accessToken": "..." }` 반환 확인 |
| 잘못된 자격증명이면 400을 반환한다 | `AuthService`가 `IllegalArgumentException` 던질 때 `400` 반환 확인 |

---

#### `CosmeticControllerTest`
**위치:** `src/test/java/com/mycosmetic/controller/CosmeticControllerTest.java`
**Mock:** `CosmeticService`, `OcrService`, `JwtUtil`, `UserDetailsServiceImpl`
**목적:** JWT 인증이 적용된 화장품 CRUD API HTTP 요청/응답 및 인증 처리 확인

| 테스트명 | 검증 내용 |
|----------|-----------|
| JWT 없이 접근하면 401을 반환한다 | 토큰 없는 요청이 `401 Unauthorized` 반환하는지 |
| 내 화장품 목록을 조회한다 | `@WithMockUser` 인증 후 `GET /cosmetics` → 200 + 목록 반환 확인 |
| 화장품을 수동 등록한다 | `POST /cosmetics` 정상 요청 → 200 + `CosmeticResponse` 반환 확인 |
| name이 없으면 400을 반환한다 | `name` 필드 누락 시 `@NotBlank` Validation → 400 반환 확인 |
| 화장품을 수정한다 | `PUT /cosmetics/{id}` 정상 요청 → 200 반환 확인 |
| 화장품을 삭제하면 204를 반환한다 | `DELETE /cosmetics/{id}` → 204 No Content 반환 확인 |

---

### 통합 테스트 (`@SpringBootTest` — 전체 컨텍스트 로드)

---

#### `SkinRecipeApplicationTests`
**위치:** `src/test/java/com/mycosmetic/SkinRecipeApplicationTests.java`
**전제 조건:** MySQL Docker 컨테이너 실행 중이어야 함
**목적:** 애플리케이션 컨텍스트가 에러 없이 정상 기동되는지 확인

| 테스트명 | 검증 내용 |
|----------|-----------|
| contextLoads() | Spring 전체 컨텍스트 (DB 연결 포함) 기동 성공 여부 |

---

## 테스트 결과 로그

### 자동화 테스트 결과 (JUnit)

#### 2026-03-24 — Phase 1 완료 후 (15개)

```
com.mycosmetic.security.JwtUtilTest                    4/4 ✅
com.mycosmetic.service.AuthServiceTest                 5/5 ✅
com.mycosmetic.controller.AuthControllerTest           5/5 ✅
com.mycosmetic.SkinRecipeApplicationTests              1/1 ✅

결과: 15/15 통과  |  누적: 15/15
```

> **비고:** `GlobalExceptionHandler` 미등록으로 `AuthControllerTest` 2개 케이스 초기 실패 (500 반환) → 추가 후 통과.

---

#### 2026-03-25 — Phase 2 완료 후 (32개)

```
com.mycosmetic.controller.CosmeticControllerTest       6/6 ✅
com.mycosmetic.service.CosmeticServiceTest             6/6 ✅
com.mycosmetic.service.OcrServiceTest                  2/2 ✅
com.mycosmetic.service.FileStorageServiceTest          3/3 ✅

결과: 17/17 통과  |  누적: 32/32
```

> **비고 1:** `@MockitoBean JwtAuthenticationFilter`로 필터 자체를 Mock하면 `chain.doFilter()` 미호출로 요청이 컨트롤러까지 도달하지 못함 → `JwtUtil` + `UserDetailsServiceImpl`만 Mock하는 방식으로 수정.
>
> **비고 2:** 인증 없는 요청이 403 반환 → `SecurityConfig`에 `AuthenticationEntryPoint` 추가해 401로 수정.
>
> **비고 3:** `UPSTAGE_API_KEY` 미설정으로 `contextLoads()` 실패 → `application.yml`에 기본값 추가.

---

### 실제 API 통합 테스트 결과 (Upstage)

**테스트 파일:** `http/test.http`
**전제 조건:** 서버 기동 + `UPSTAGE_API_KEY` 환경변수 설정 + MySQL 컨테이너 실행 중

#### 2026-03-25 — Phase 2 OCR 파이프라인 테스트

| 제품 | 브랜드 | 결과 | 비고 |
|------|--------|------|------|
| R.E.D BLEMISH For Men Calming All In One | Dr.G | high → 자동저장 ✅ | 제품명에 설명 텍스트 혼입, category 오분류 (LLM 튜닝 필요) |
| Heartleaf Calming Toner Skin Booster | Abib | high → 자동저장 ✅ | brand "A 9 ib" 오인식 (LLM 튜닝 필요) |
| Heartleaf Calming Moisture Sun Cream | goodal | low → HITL confirm ✅ | confidence 비결정적 — 실행마다 high/low 혼재 |

**HITL 확인 저장:** `POST /cosmetics/ocr/confirm` 정상 동작 확인 ✅

#### 발견 이슈 및 수정 내역

| 이슈 | 원인 | 수정 |
|------|------|------|
| OCR 텍스트 빈 문자열 반환 | Upstage 응답의 `text` 필드가 비어있고 텍스트는 `html` 필드에만 존재 | `parseText()`를 `content.html` 파싱으로 변경 |
| 429 Too Many Requests | Upstage Tier 0 RPS 1 제한 — 앞면/뒷면 연속 요청 시 초과 | 두 OCR 요청 사이에 1.1초 딜레이 추가 |
| category Enum 역직렬화 실패 | LLM이 유효하지 않은 문자열 반환 시 Jackson InvalidFormatException | `OcrParseResult.category`를 `String`으로 변경 + ETC fallback |
