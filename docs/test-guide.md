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
./gradlew test --tests "com.mycosmetic.service.InMemoryVectorStoreServiceTest"
./gradlew test --tests "com.mycosmetic.service.RoutineServiceTest"
./gradlew test --tests "com.mycosmetic.service.ChatServiceTest"
./gradlew test --tests "com.mycosmetic.controller.ChatControllerTest"

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
**Mock:** `UserRepository`, `CosmeticRepository`, `RoutineRepository`, `ChatSessionRepository`, `VectorStoreService`, `BCryptPasswordEncoder`, `JwtUtil`
**목적:** 회원가입 / 로그인 / 회원 탈퇴 비즈니스 로직 확인

| 테스트명 | 검증 내용 |
|----------|-----------|
| 정상적인 회원가입 요청이면 User가 저장된다 | `userRepository.save()` 1회 호출 확인 |
| 이미 존재하는 이메일이면 예외가 발생한다 | `existsByEmail = true` 일 때 `IllegalArgumentException` 발생 + `save()` 미호출 확인 |
| 올바른 이메일/비밀번호이면 JWT가 반환된다 | 로그인 성공 시 `accessToken` 포함된 `LoginResponse` 반환 확인 |
| 존재하지 않는 이메일이면 예외가 발생한다 | `findByEmail = empty` 일 때 `IllegalArgumentException` 발생 확인 |
| 비밀번호가 틀리면 예외가 발생한다 | `passwordEncoder.matches = false` 일 때 `IllegalArgumentException` 발생 확인 |
| 회원 탈퇴 시 연관 데이터가 순서대로 삭제된다 | `chatSessionRepository.deleteAll()` → `routineRepository.deleteAll()` → `vectorStoreService.removeVector()` → `cosmeticRepository.deleteAll()` → `userRepository.delete()` 순서 호출 확인 |

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

#### `InMemoryVectorStoreServiceTest`
**위치:** `src/test/java/com/mycosmetic/service/InMemoryVectorStoreServiceTest.java`
**Mock:** `UpstageEmbeddingClient`
**목적:** 코사인 유사도 기반 벡터 검색 로직, 저장/삭제/덮어쓰기 동작 확인

| 테스트명 | 검증 내용 |
|----------|-----------|
| addVector 후 search하면 해당 ID가 반환된다 | 벡터 추가 후 동일 방향 쿼리로 검색 시 해당 ID 반환 확인 |
| removeVector 후 search하면 해당 ID가 반환되지 않는다 | 제거 후 store 비어 있으면 embed 미호출 + 빈 리스트 반환 확인 |
| 쿼리와 유사도가 높은 순서로 결과가 정렬된다 | `[1,0]`, `[1,1]`, `[0,1]` 벡터 저장 후 `[1,0]` 쿼리 시 유사도 내림차순 정렬 확인 |
| topK가 저장된 벡터 수보다 작으면 topK개만 반환된다 | 3개 저장 후 topK=2 검색 시 상위 2개만 반환 확인 |
| 벡터 스토어가 비어 있으면 빈 리스트를 반환한다 | 빈 store 검색 시 embed 미호출 + 빈 리스트 반환 확인 |
| addVector는 동일 ID로 재호출 시 벡터를 덮어쓴다 | 동일 ID 재저장 후 새 벡터 기준으로 검색 결과 반영 확인 |

---

#### `RoutineServiceTest`
**위치:** `src/test/java/com/mycosmetic/service/RoutineServiceTest.java`
**Mock:** `RoutineRepository`, `CosmeticRepository`, `UserRepository`, `UpstageLlmClient`
**목적:** 루틴 생성(LLM 연동), 조회, 삭제, 소유자 검증, 엣지 케이스 확인

| 테스트명 | 검증 내용 |
|----------|-----------|
| 보유 화장품이 있으면 LLM 추천으로 루틴이 생성된다 | LLM 응답 기반으로 `Routine` 저장 + `RoutineResponse` 필드(name, timeOfDay, steps) 확인 |
| 보유 화장품이 없으면 예외가 발생한다 | 화장품 0개 시 `IllegalStateException` 발생 + LLM/save 미호출 확인 |
| LLM이 존재하지 않는 화장품 ID를 반환하면 해당 step은 무시된다 | 목록에 없는 ID 반환 시 steps 비어 있는지 확인 |
| 내 루틴 목록을 조회한다 | `findAllByUserId()` 결과가 `RoutineResponse` 리스트로 반환되는지 |
| 내 루틴을 삭제한다 | `routineRepository.delete()` 1회 호출 확인 |
| 존재하지 않는 루틴 삭제 시 예외가 발생한다 | `findById = empty` 시 `IllegalArgumentException` + delete 미호출 확인 |
| 다른 사람의 루틴을 삭제하면 예외가 발생한다 | 소유자 불일치 시 `IllegalArgumentException` + delete 미호출 확인 |

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

#### `ChatServiceTest`
**위치:** `src/test/java/com/mycosmetic/service/ChatServiceTest.java`
**Mock:** `ChatSessionRepository`, `ChatMessageRepository`, `CosmeticRepository`, `RoutineRepository`, `VectorStoreService`, `UpstageLlmClient`, `UserRepository`
**목적:** 세션 CRUD, RAG 파이프라인, 루틴 컨텍스트 주입, 히스토리 제한, 소유자 검증 확인

| 테스트명 | 검증 내용 |
|----------|-----------|
| 세션을 생성하면 ChatSessionResponse를 반환한다 | `saveAndFlush()` 1회 호출 + `id`, `createdAt` 포함 응답 확인 |
| 세션 목록을 조회하면 최신순으로 반환된다 | `findAllByUserIdOrderByCreatedAtDesc()` 결과가 `ChatSessionResponse` 리스트로 반환되는지 |
| 세션을 삭제한다 | `chatSessionRepository.delete()` 1회 호출 확인 |
| 다른 사람의 세션을 삭제하면 예외가 발생한다 | 소유자 불일치 시 `IllegalArgumentException` + delete 미호출 확인 |
| 존재하지 않는 세션을 삭제하면 예외가 발생한다 | `findById = empty` 시 `IllegalArgumentException` 확인 |
| 관련 화장품이 있으면 RAG 컨텍스트가 포함된 프롬프트로 LLM을 호출한다 | `vectorStore.search()` 결과 기반으로 `cosmeticRepository.findAllById()` 호출 + LLM 프롬프트에 화장품 정보 포함 확인 |
| 관련 화장품이 없어도 피부 정보는 항상 시스템 프롬프트에 포함된다 | `vectorStore.search()` 빈 리스트 반환 시에도 피부 타입·고민·알레르기 정보가 프롬프트에 포함되는지 확인 |
| 루틴이 있으면 시스템 프롬프트에 루틴 정보가 포함된다 | `findAllByUserIdWithCosmetics()` 결과가 `ArgumentCaptor`로 캡처한 프롬프트에 `[보유 루틴]`·루틴명·화장품명으로 포함되는지 확인 |
| 루틴이 없으면 시스템 프롬프트에 루틴 섹션이 포함되지 않는다 | 빈 리스트 반환 시 프롬프트에 `[보유 루틴]` 문자열 없음 확인 |
| 히스토리가 10개를 초과하면 최근 10개만 LLM에 전달된다 | 메시지 12개 저장 후 LLM 호출 시 `ArgumentCaptor`로 history 크기가 10임을 확인 |
| 채팅 후 USER 메시지와 ASSISTANT 메시지가 저장된다 | `chatMessageRepository.save()` 2회 호출 + 각 role 확인 |
| 채팅 후 ChatResponse에 LLM 응답이 담긴다 | `upstageLlmClient.chat()` 반환값이 `ChatResponse.answer`에 담기는지 확인 |
| 히스토리를 조회한다 | `findAllBySessionIdOrderByCreatedAtAsc()` 결과가 `ChatMessageResponse` 리스트로 반환되는지 |
| 다른 사람의 세션 히스토리를 조회하면 예외가 발생한다 | 소유자 불일치 시 `IllegalArgumentException` 발생 확인 |

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

#### `ChatControllerTest`
**위치:** `src/test/java/com/mycosmetic/controller/ChatControllerTest.java`
**Mock:** `ChatService`, `JwtUtil`, `UserDetailsServiceImpl`
**목적:** JWT 인증이 적용된 채팅 세션 API HTTP 요청/응답 및 인증 처리 확인

| 테스트명 | 검증 내용 |
|----------|-----------|
| JWT 없이 세션 생성 요청하면 401을 반환한다 | 토큰 없는 요청이 `401 Unauthorized` 반환하는지 |
| 세션을 생성하면 200을 반환한다 | `POST /chat/sessions` 정상 요청 → 200 + `ChatSessionResponse` 반환 확인 |
| 세션 목록을 조회하면 200을 반환한다 | `GET /chat/sessions` 정상 요청 → 200 + 리스트 반환 확인 |
| 세션을 삭제하면 204를 반환한다 | `DELETE /chat/sessions/{id}` → 204 No Content 반환 확인 |
| 채팅 메시지를 보내면 200을 반환한다 | `POST /chat/sessions/{id}/messages` 정상 요청 → 200 + `ChatResponse` 반환 확인 |
| message가 빈 문자열이면 400을 반환한다 | `message: ""` 전송 시 `@NotBlank` Validation → 400 반환 확인 |
| 히스토리를 조회하면 200을 반환한다 | `GET /chat/sessions/{id}/messages` 정상 요청 → 200 + 리스트 반환 확인 |

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

#### 2026-03-25 — Phase 3 완료 후 (45개)

```
com.mycosmetic.service.InMemoryVectorStoreServiceTest  6/6 ✅
com.mycosmetic.service.RoutineServiceTest              7/7 ✅

결과: 13/13 통과  |  누적: 45/45
```

> **비고 1:** `CosmeticService`에 `VectorStoreService` 의존성 추가로 기존 `CosmeticServiceTest` NPE 발생 → `@Mock VectorStoreService` 추가로 해결.
>
> **비고 2:** `ApplicationReadyEvent`에서 시작 시 전체 화장품 임베딩 재로드 시 테스트 DB에 데이터가 있으면 유효하지 않은 API 키로 401 발생 → try-catch로 임베딩 실패 건너뜀 처리.
>
> **비고 3:** `removeVector` 후 빈 store 검색 시 `embed("query")` 미호출됨 → 불필요한 스텁 선언 제거 (Mockito strict 모드 `UnnecessaryStubbingException`).

---

#### 2026-03-25 — Phase 4 완료 후 (64개)

```
com.mycosmetic.service.ChatServiceTest                 12/12 ✅
com.mycosmetic.controller.ChatControllerTest            7/7  ✅

결과: 19/19 통과  |  누적: 64/64
```

---

#### 2026-03-27 — 챗봇 루틴 컨텍스트 + 테스트 버그 수정 (68개)

```
com.mycosmetic.service.ChatServiceTest                 14/14 ✅  (+2 신규)

결과: 4/4 통과  |  누적: 68/68
```

> **비고 1 (신규):** `ChatServiceTest`에 루틴 컨텍스트 주입 테스트 2개 추가 — `RoutineRepository` Mock 및 `makeRoutine()` 헬퍼 추가.
>
> **비고 2 (버그 수정):** `ChatServiceTest.createSession` — `verify(chatSessionRepository.save(...))` → `verify(chatSessionRepository.saveAndFlush(...))` 불일치 수정 (`saveAndFlush`로 구현 변경됐으나 테스트 미반영).
>
> **비고 3 (버그 수정):** `InMemoryVectorStoreServiceTest` — `embeddingClient.embed(String)` 단일 인자 호출 → `embedPassage()` / `embedQuery()` 분리 모델로 전환 후 테스트 미반영 수정.

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

---

#### 2026-03-25 — Phase 3 루틴 추천 테스트

| 요청 | 결과 | 비고 |
|------|------|------|
| `POST /routines` (AM) | ✅ 200 | name="아침 루틴", steps 2개 (Dr.G + goodal 선크림) |
| `POST /routines` (PM) | ✅ 200 | name="저녁 루틴", steps 2개 |
| `GET /routines` | ✅ 200 | AM/PM 루틴 목록 정상 반환 |
| `DELETE /routines/{id}` | ✅ 204 | 정상 삭제 확인 |

#### 발견 이슈 및 수정 내역

| 이슈 | 원인 | 수정 |
|------|------|------|
| PM 루틴 name이 "아침 루틴"으로 반환 | LLM이 `timeOfDay`를 이름에 반영하지 않음 | 프롬프트에 `AM이면 "아침 루틴", PM이면 "저녁 루틴"` 규칙 명시 |
| `test.http` 삭제 요청 400 반환 | `Authorization` 헤더 오타 (`Autjshorization`) | `test.http` 오타 수정 |

---

#### 2026-03-25 — Phase 4 채팅 세션 & RAG 테스트

| 요청 | 결과 | 비고 |
|------|------|------|
| `POST /chat/sessions` | ✅ 200 | 세션 생성 + `createdAt` 정상 반환 |
| `GET /chat/sessions` | ✅ 200 | 세션 목록 최신순 반환 |
| `DELETE /chat/sessions/{id}` | ✅ 204 | cascade 삭제 (chat_messages → chat_sessions) 확인 |
| `POST /chat/sessions/{id}/messages` (RAG) | ✅ 200 | 보유 화장품(Dr.G, Abib, goodal) 기반 응답 확인 |
| `GET /chat/sessions/{id}/messages` | ✅ 200 | 히스토리 메시지 순서대로 반환 |

#### 발견 이슈 및 수정 내역

| 이슈 | 원인 | 수정 |
|------|------|------|
| `createdAt: null` in create response | `@CreationTimestamp`는 Hibernate flush 시점에 설정되나 `save()` 반환 객체에는 반영 안 됨 | `save()` → `saveAndFlush()` 변경 |
| 벡터 스토어 초기화 400 Bad Request | `solar-embedding-1-large` 모델 deprecated | `application.yml`을 `solar-embedding-1-large-passage` / `solar-embedding-1-large-query`로 분리 |
| 채팅 첫 요청 500 ClassCastException | Jackson이 정수값 임베딩을 `Integer`로 역직렬화 → `List<Double>` 캐스팅 실패 | `List<?>` + `((Number) elem).floatValue()` 로 변경 |
