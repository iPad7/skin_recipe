# Skin Recipe — 작업 명세서

## Phase 1 — 회원 관리 + JWT

---

### T1-1. `User` 엔티티 + `SkinType` Enum

**목표:** DB `user` 테이블과 매핑되는 JPA 엔티티 클래스 작성

**작업 내용:**
- `SkinType` Enum 작성: `DRY / OILY / COMBINATION / SENSITIVE / NORMAL`
- `User` 엔티티 필드: `id`, `email`, `password`, `nickname`, `skinType`, `skinConcerns`, `allergyIngredients`, `createdAt`
- `skinConcerns`는 `String` 타입으로 콤마 구분 저장 (예: `"ACNE,PORE"`)
- `@CreationTimestamp`로 `createdAt` 자동 설정
- Lombok `@Getter`, `@NoArgsConstructor`, `@Builder` 적용

**생성 파일:**
- `entity/SkinType.java`
- `entity/User.java`

---

### T1-2. `UserRepository`

**목표:** User 엔티티에 대한 DB 접근 인터페이스 작성

**작업 내용:**
- `JpaRepository<User, Long>` 상속
- `findByEmail(String email)` 메서드 선언 (로그인/중복 확인에 사용)
- `existsByEmail(String email)` 메서드 선언 (회원가입 중복 체크)

**생성 파일:**
- `repository/UserRepository.java`

---

### T1-3. 회원가입 API — `POST /auth/signup`

**목표:** 이메일/비밀번호/피부 정보를 받아 User를 DB에 저장

**Request Body (`SignupRequest`):**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "홍길동",
  "skinType": "DRY",
  "skinConcerns": "ACNE,PORE",
  "allergyIngredients": "향료, 알코올"
}
```

**Response (`MessageResponse`):**
```json
{ "message": "회원가입이 완료되었습니다." }
```

**작업 내용:**
- `SignupRequest` DTO 작성 + `@NotBlank`, `@Email` Validation 적용
- `AuthService.signup()`: 이메일 중복 확인 → `BCryptPasswordEncoder`로 비밀번호 암호화 → 저장
- `AuthController.signup()`: `POST /auth/signup` 엔드포인트

**생성 파일:**
- `dto/request/SignupRequest.java`
- `dto/response/MessageResponse.java`
- `service/AuthService.java`
- `controller/AuthController.java`

---

### T1-4. 로그인 API — `POST /auth/login`

**목표:** 이메일/비밀번호 검증 후 JWT 액세스 토큰 반환

**Request Body (`LoginRequest`):**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (`LoginResponse`):**
```json
{ "accessToken": "eyJhbGci..." }
```

**작업 내용:**
- `LoginRequest` DTO 작성
- `LoginResponse` DTO 작성
- `AuthService.login()`: 이메일 조회 → 비밀번호 일치 확인 → JWT 생성 후 반환
- `AuthController.login()`: `POST /auth/login` 엔드포인트

**생성 파일:**
- `dto/request/LoginRequest.java`
- `dto/response/LoginResponse.java`
- (AuthService, AuthController는 T1-3에서 이미 생성, 메서드 추가)

---

### T1-5. JWT 유틸 클래스

**목표:** JWT 토큰 생성 / 파싱 / 검증 로직을 한 곳에 모음

**작업 내용:**
- `application.yml`에 `jwt.secret`, `jwt.expiration` 설정 추가
- `JwtUtil` 클래스 작성:
  - `generateToken(String email)`: 토큰 생성 (만료 시간 포함)
  - `getEmailFromToken(String token)`: 토큰에서 이메일 추출
  - `validateToken(String token)`: 만료/위변조 검증, boolean 반환

**생성 파일:**
- `security/JwtUtil.java`

---

### T1-6. Request/Response DTO + Validation 정리

**목표:** T1-3, T1-4에서 만든 DTO에 입력값 검증 어노테이션을 일관성 있게 적용

**작업 내용:**
- `SignupRequest`: `@NotBlank`, `@Email`, `@NotNull` 적용
- `LoginRequest`: `@NotBlank`, `@Email` 적용
- 컨트롤러 파라미터에 `@Valid` 추가
- Validation 실패 시 400 응답 반환 확인

---

## Phase 2 — Spring Security + Cosmetic CRUD + OCR

---

### T2-1. Spring Security 설정 — JWT 필터 체인

**목표:** 모든 요청에서 JWT를 검증하고, 인증된 사용자 정보를 SecurityContext에 저장

**작업 내용:**
- `SecurityConfig` 작성:
  - `/auth/**` 경로는 인증 없이 허용 (permitAll)
  - 나머지 경로는 JWT 인증 필요
  - CSRF 비활성화, Session Stateless 설정
- `JwtAuthenticationFilter` 작성:
  - 요청 헤더 `Authorization: Bearer <token>` 파싱
  - `JwtUtil.validateToken()` → `UsernamePasswordAuthenticationToken` 생성 → SecurityContext 저장
- `UserDetailsServiceImpl` 작성: 이메일로 User 조회

**생성 파일:**
- `config/SecurityConfig.java`
- `security/JwtAuthenticationFilter.java`
- `security/UserDetailsServiceImpl.java`

---

### T2-2. `Cosmetic` 엔티티 + `CosmeticCategory` Enum

**목표:** DB `cosmetic` 테이블과 매핑되는 JPA 엔티티 작성

**작업 내용:**
- `CosmeticCategory` Enum: `SKIN / ESSENCE / CREAM / SUNSCREEN / CLEANSING / ETC`
- `Cosmetic` 엔티티 필드: `id`, `user`, `name`, `brand`, `category`, `ingredients`, `imageUrl`, `createdAt`
- `user` 필드는 `@ManyToOne`으로 User와 연관관계 설정
- `CosmeticRepository` 작성: `findAllByUserId(Long userId)` 메서드 포함

**생성 파일:**
- `entity/CosmeticCategory.java`
- `entity/Cosmetic.java`
- `repository/CosmeticRepository.java`

---

### T2-3. 화장품 CRUD API

**목표:** 로그인한 사용자의 화장품 목록 조회, 수동 등록, 수정, 삭제

**엔드포인트:**
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/cosmetics` | 내 화장품 목록 조회 |
| POST | `/cosmetics` | 수동 등록 |
| PUT | `/cosmetics/{id}` | 수정 |
| DELETE | `/cosmetics/{id}` | 삭제 |

**작업 내용:**
- `CosmeticRequest` DTO (name, brand, category, ingredients)
- `CosmeticResponse` DTO
- `CosmeticService`: CRUD 메서드 + 소유자 검증 (다른 사용자의 화장품 접근 차단)
- `CosmeticController`: 위 4개 엔드포인트, `@AuthenticationPrincipal`로 현재 사용자 추출

**생성 파일:**
- `dto/request/CosmeticRequest.java`
- `dto/response/CosmeticResponse.java`
- `service/CosmeticService.java`
- `controller/CosmeticController.java`

---

### T2-4. 이미지 업로드 — 로컬 저장

**목표:** 멀티파트 이미지 파일을 서버 `/uploads/` 디렉토리에 저장하고 URL 반환

**작업 내용:**
- `FileStorageService` 작성:
  - UUID 기반 파일명 생성으로 충돌 방지
  - `application.yml`의 `file.upload-dir` 경로에 저장
  - 저장 경로 문자열 반환
- OCR 요청 시 이미지 먼저 저장 후 OCR 호출

**생성 파일:**
- `service/FileStorageService.java`

---

### T2-5. Upstage OCR API 연동 — `POST /cosmetics/ocr`

**목표:** 앞면 + 뒷면 이미지 2장을 받아 Upstage Document Parse로 텍스트 추출

**Request:** `multipart/form-data` — `frontImage`, `backImage` 파일 2개

**작업 내용:**
- `UpstageOcrClient` 작성 (WebClient 기반):
  - Upstage Document Parse API 호출 (`ocr=force`)
  - 응답에서 텍스트 추출
- 앞면 + 뒷면 텍스트를 합쳐 Solar LLM 파싱으로 넘김 (T2-6 연결)

**생성 파일:**
- `service/UpstageOcrClient.java`

---

### T2-6. Solar LLM 파싱 — 제품명/브랜드/성분/confidence 추출

**목표:** OCR 텍스트를 Solar LLM에 전달해 구조화된 화장품 정보 파싱

**Response (`OcrParseResult`):**
```json
{
  "name": "토너",
  "brand": "이니스프리",
  "ingredients": "정제수, 부틸렌글라이콜, ...",
  "confidence": "high"
}
```

**작업 내용:**
- `UpstageLlmClient` 작성 (WebClient 기반): Solar Chat Completion API 호출
- 파싱 전용 system 프롬프트 작성 (JSON 형식 출력 지시)
- `OcrParseResult` DTO 작성
- `confidence: high` → 자동 저장 + 벡터 동기화 (Phase 3 연결)
- `confidence: low` → `OcrParseResult` 그대로 반환 (사용자 확인 대기)

**생성 파일:**
- `service/UpstageLlmClient.java`
- `dto/response/OcrParseResult.java`

---

## Phase 3 — HITL + Routine + VectorStore

---

### T3-1. HITL 확인 저장 API — `POST /cosmetics/ocr/confirm`

**목표:** confidence: low 결과를 사용자가 확인/수정 후 최종 저장

**Request Body (`OcrConfirmRequest`):**
```json
{
  "name": "토너",
  "brand": "이니스프리",
  "category": "SKIN",
  "ingredients": "정제수, ...",
  "imageUrl": "/uploads/abc.jpg"
}
```

**작업 내용:**
- `OcrConfirmRequest` DTO 작성
- `CosmeticService.confirmOcr()`: 저장 + 벡터 동기화 호출
- `CosmeticController`에 엔드포인트 추가

**생성 파일:**
- `dto/request/OcrConfirmRequest.java`

---

### T3-2. `VectorStoreService` 인터페이스 + `InMemoryVectorStoreService` 구현

**목표:** 전략 패턴으로 벡터 스토어 구현체를 교체 가능하게 설계

**인터페이스:**
```java
public interface VectorStoreService {
    void addVector(Long cosmeticId, String text);
    void removeVector(Long cosmeticId);
    List<Long> search(String query, int topK);
}
```

**작업 내용:**
- `VectorStoreService` 인터페이스 작성
- `InMemoryVectorStoreService` 구현:
  - `ConcurrentHashMap<Long, float[]>` 으로 벡터 저장
  - 코사인 유사도로 topK 검색
  - `@Primary` 지정
- 임베딩 생성은 T3-3의 `UpstageEmbeddingClient` 주입받아 사용

**생성 파일:**
- `service/VectorStoreService.java`
- `service/InMemoryVectorStoreService.java`

---

### T3-3. Upstage Embedding API 연동

**목표:** 텍스트를 벡터(float[])로 변환하는 클라이언트 작성

**작업 내용:**
- `UpstageEmbeddingClient` 작성 (WebClient 기반):
  - Upstage Embedding API (`solar-embedding-1-large`) 호출
  - 응답에서 `float[]` 추출 후 반환

**생성 파일:**
- `service/UpstageEmbeddingClient.java`

---

### T3-4. 화장품 저장/삭제 시 벡터 자동 동기화

**목표:** 화장품 DB 저장/삭제 시 인메모리 벡터도 자동으로 갱신

**작업 내용:**
- `CosmeticService.save()` 내에 `VectorStoreService.addVector()` 호출 추가
  - 임베딩 대상 텍스트: `name + " " + brand + " " + ingredients`
- `CosmeticService.delete()` 내에 `VectorStoreService.removeVector()` 호출 추가
- 앱 시작 시 DB 전체 화장품을 벡터 스토어에 로드하는 초기화 로직 추가 (`@EventListener ApplicationReadyEvent`)

---

### T3-5. `Routine`, `RoutineCosmetic` 엔티티

**목표:** 루틴 및 루틴-화장품 연결 테이블 엔티티 작성

**작업 내용:**
- `TimeOfDay` Enum: `AM / PM`
- `Routine` 엔티티: `id`, `user`, `name`, `timeOfDay`, `description`, `createdAt`
- `RoutineCosmetic` 엔티티: `id`, `routine`, `cosmetic`, `order`
- `RoutineRepository`, `RoutineCosmeticRepository` 작성

**생성 파일:**
- `entity/TimeOfDay.java`
- `entity/Routine.java`
- `entity/RoutineCosmetic.java`
- `repository/RoutineRepository.java`
- `repository/RoutineCosmeticRepository.java`

---

### T3-6. 루틴 추천 API — `POST /routines`

**목표:** Solar LLM이 사용자의 보유 화장품 중 AM/PM에 맞는 제품과 순서를 추천

**Request Body:**
```json
{ "timeOfDay": "AM" }
```

**작업 내용:**
- `RoutineService.create()`:
  1. 사용자의 전체 화장품 조회
  2. Solar LLM에 피부 정보 + 화장품 목록 전달 → 추천 순서 및 설명 응답
  3. LLM 응답 파싱 → `Routine` + `RoutineCosmetic` 저장
- `UpstageLlmClient`에 루틴 추천용 메서드 추가

**생성 파일:**
- `service/RoutineService.java`
- `controller/RoutineController.java`
- `dto/request/RoutineRequest.java`
- `dto/response/RoutineResponse.java`

---

### T3-7. 루틴 조회/삭제 API

**목표:** 저장된 루틴 목록 조회 및 삭제

**엔드포인트:**
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/routines` | 루틴 목록 조회 |
| DELETE | `/routines/{id}` | 루틴 삭제 |

**작업 내용:**
- `RoutineService.findAll()`, `RoutineService.delete()` 구현
- 삭제 시 `RoutineCosmetic` 연쇄 삭제 처리

---

## Phase 4 — RAG 챗봇

---

### T4-1. `ChatSession` + `ChatMessage` 엔티티

**목표:** 세션 기반 대화 히스토리 저장용 엔티티 작성

**작업 내용:**
- `Role` Enum: `USER / ASSISTANT`
- `ChatSession` 엔티티: `id (UUID)`, `user`, `createdAt`
  - `@OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)`로 메시지 연쇄 삭제
- `ChatMessage` 엔티티: `id`, `session`, `role`, `content`, `createdAt`
- `ChatSessionRepository`: `findAllByUserIdOrderByCreatedAtDesc()` 메서드 포함
- `ChatMessageRepository`: `findAllBySessionIdOrderByCreatedAtAsc()` 메서드 포함

**생성 파일:**
- `entity/Role.java`
- `entity/ChatSession.java`
- `entity/ChatMessage.java`
- `repository/ChatSessionRepository.java`
- `repository/ChatMessageRepository.java`

---

### T4-2. 세션 관리 API

**목표:** 대화 세션 생성 / 목록 조회 / 삭제

**엔드포인트:**

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/chat/sessions` | 새 세션 생성 |
| GET | `/chat/sessions` | 내 세션 목록 조회 |
| DELETE | `/chat/sessions/{sessionId}` | 세션 삭제 (메시지 연쇄 삭제) |

**작업 내용:**
- `ChatSessionResponse` DTO 작성
- `ChatService.createSession()`, `findAllSessions()`, `deleteSession()` 구현
- `ChatController`에 위 3개 엔드포인트 추가

**생성 파일:**
- `dto/response/ChatSessionResponse.java`
- `service/ChatService.java`
- `controller/ChatController.java`

---

### T4-3. 챗봇 API — `POST /chat/sessions/{sessionId}/messages`

**목표:** 세션 내에서 질문을 받아 RAG 파이프라인을 실행하고 개인화된 답변 반환

**Request Body:**
```json
{ "message": "내 피부에 맞는 수분크림 추천해줘" }
```

**Response:**
```json
{ "answer": "보유하신 제품 중에서 ..." }
```

**RAG 파이프라인 (`ChatService` 내부):**
1. 질문 텍스트 → `UpstageEmbeddingClient`로 벡터화
2. `VectorStoreService.search()` → 관련 화장품 ID 3개 조회
3. ID로 `CosmeticRepository`에서 화장품 상세 정보 조회
4. `User` 엔티티에서 피부 정보 로드
5. system 프롬프트 조립 (`ChatService` 책임):
   - 사용자 피부타입 / 피부고민 / 알레르기 성분
   - 관련 화장품 3개의 이름, 브랜드, 성분
6. 해당 세션의 최근 10개 메시지 로드
7. `UpstageLlmClient.chat(systemPrompt, history, userMessage)` 호출
8. 사용자 질문 + AI 답변을 `ChatMessage`에 저장

**`UpstageLlmClient` 추가 메서드:**
```java
String chat(String systemPrompt, List<ChatMessage> history, String userMessage)
```
- `ChatService`가 프롬프트를 조립해서 전달 (클라이언트는 LLM 통신만 담당)
- `history`를 `[{"role": "user"/"assistant", "content": "..."}]`로 변환 후 전송

**생성 파일:**
- `dto/request/ChatRequest.java`
- `dto/response/ChatResponse.java`

---

### T4-4. 대화 히스토리 API — `GET /chat/sessions/{sessionId}/messages`

**목표:** 특정 세션의 전체 대화 기록 반환

**작업 내용:**
- `ChatService.getHistory()`: 세션 ID로 메시지 조회 (시간순)
- `ChatController`에 `GET /chat/sessions/{sessionId}/messages` 엔드포인트 추가
- `ChatMessageResponse` DTO 작성

**생성 파일:**
- `dto/response/ChatMessageResponse.java`

---

### T4-5. 전역 예외 처리 확인

**목표:** Phase 1에서 선행 작성된 `GlobalExceptionHandler` 커버리지 확인

**작업 내용:**
- `IllegalArgumentException` → 400, `UsernameNotFoundException` → 404, `Exception` → 500 확인
- 신규 엔드포인트에서 누락된 케이스 있으면 보완

---

## Phase 5 — 중간 결산

> 프론트엔드 개발 전 백엔드 완성 시점의 문서 정리. README 최종본은 Phase 9에서 완성.

---

### T5-1. ERD 정리

**목표:** 최종 DB 스키마를 ERD로 문서화

**작업 내용:**
- `docs/erd.md`에 Mermaid 다이어그램으로 작성 (GitHub 렌더링용)
- dbdiagram.io용 DBML 코드 함께 제공
- 대상 엔티티: User / Cosmetic / Routine / RoutineCosmetic / ChatSession / ChatMessage

**생성 파일:**
- `docs/erd.md`

---

### T5-2. API 정의서 작성

**목표:** 프론트엔드 개발 시 참조할 API 명세 문서화

**작업 내용:**
- 전체 엔드포인트 Request / Response 스펙 정리
- 인증 방식 (Bearer JWT) 명시
- 에러 응답 형식 정리

**생성 파일:**
- `docs/api-spec.md`

---

## Phase 6 — 프론트엔드

> React + Vite 기반 SPA. 백엔드 API 연동 포함. 위치: `frontend/`

---

### T6-1. React + Vite 프로젝트 세팅

**목표:** 프론트엔드 프로젝트 초기 구성 및 백엔드 연동 환경 설정

**작업 내용:**
- `frontend/` 디렉토리에 Vite + React 프로젝트 생성
- axios 또는 fetch 기반 API 클라이언트 설정
- JWT 토큰 localStorage 관리
- 라우팅 설정 (react-router-dom)
- 백엔드 CORS 설정 추가

---

### T6-2. 회원가입 / 로그인 페이지

**목표:** 인증 흐름 구현

**작업 내용:**
- 회원가입 폼 (이메일, 비밀번호, 닉네임, 피부타입, 피부고민, 알레르기 성분)
- 로그인 폼 → JWT 저장 → 메인 페이지 이동
- 인증 필요 페이지 접근 시 로그인 페이지로 리다이렉트

---

### T6-3. 화장품 관리 페이지

**목표:** 화장품 목록 조회, 수동 등록, OCR 등록, 수정, 삭제

**작업 내용:**
- 화장품 목록 카드 UI
- 수동 등록 폼
- OCR 등록: 카메라/파일 업로드 → 앞면·뒷면 2장 → 결과 확인 (HITL 포함)
- 수정 / 삭제

---

### T6-4. 루틴 페이지

**목표:** AM/PM 루틴 생성, 조회, 삭제

**작업 내용:**
- AM / PM 탭 구분
- 루틴 생성 버튼 → LLM 추천 결과 표시
- 루틴 카드 (제품 순서, 설명)
- 삭제

---

### T6-5. 챗봇 페이지

**목표:** 세션 기반 RAG 챗봇 UI

**작업 내용:**
- 세션 목록 사이드바 (생성 / 선택 / 삭제)
- 채팅 인터페이스 (메시지 입력 → 전송 → AI 응답 표시)
- 히스토리 로드

---

## Phase 7 — 배포

---

### T7-1. RDS MySQL 설정

**목표:** AWS RDS t3.micro (프리티어) MySQL 인스턴스 생성

**작업 내용:**
- RDS 인스턴스 생성 (MySQL 8, t3.micro)
- 보안 그룹 설정 (EC2에서만 접근 허용)
- `application-prod.yml` 작성

---

### T7-2. EC2 배포 — Spring Boot

**목표:** EC2 t2.micro에 Spring Boot 서버 배포

**작업 내용:**
- EC2 인스턴스 생성 (Amazon Linux 2, t2.micro)
- Java 21 설치
- JAR 빌드 + 업로드 + 실행 (systemd 서비스 등록)
- 환경변수 설정 (`UPSTAGE_API_KEY` 등)
- 보안 그룹 설정 (8080 포트 오픈)

---

### T7-3. Vercel 배포 — React

**목표:** Vercel에 React 프론트엔드 배포

**작업 내용:**
- Vercel 프로젝트 연결 (GitHub 레포 `frontend/` 루트 지정)
- 환경변수 설정 (`VITE_API_BASE_URL` = EC2 주소)
- 프로덕션 CORS 설정 (백엔드에 Vercel 도메인 허용)

---

## Phase 8 — 인수 테스트

---

### T8-1. 전체 사용자 흐름 검증

**목표:** 배포된 환경에서 실제 사용자 흐름 전체를 수동으로 검증

**작업 내용:**
- 회원가입 → 로그인
- 화장품 OCR 등록 (카메라 촬영)
- HITL 확인 저장
- 루틴 생성 (AM/PM)
- 챗봇 대화 (RAG 응답 확인)
- 세션 삭제 / 화장품 삭제 (cascade 확인)

---

## Phase 9 — 마무리

---

### T9-1. README 최종본

**목표:** 배포 URL 포함 최종 README 작성

**작업 내용:**
- 프로젝트 소개 + 동작 흐름
- 기술 스택 (프론트엔드 포함)
- 실행 방법 (로컬 + 배포)
- API 엔드포인트 목록
- 아키텍처 핵심 설명 (SOT 분리, 전략 패턴, RAG)
- 개발 일정 (Phase 1~9)

---

### T9-2. GitHub 최종 제출

**목표:** 레포 정리 후 부트캠프 지원서에 제출

**작업 내용:**
- 불필요한 파일 정리 (.idea 등 .gitignore 확인)
- 최종 커밋 + 태그
- GitHub 레포 public 확인
