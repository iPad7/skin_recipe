# PRD — Skin Recipe (가)

## 1. 배경 및 목적

Skin Recipe는 내 화장품만을 다룬다. 사진 한 장으로 성분표까지 자동 등록하고, 보유 화장품과 내 피부 정보를 바탕으로 개인화된 루틴 추천과 챗봇 Q&A를 제공한다.

---

## 2. 사용자 정의

단일 사용자 타입. 회원가입 시 아래 피부 정보를 입력한다.

| 항목 | 형식 | 예시 |
|------|------|------|
| 피부 타입 | Enum 단일 선택 | 건성 / 지성 / 복합성 / 민감성 / 중성 |
| 피부 고민 | String 다중선택 (콤마 구분) | ACNE, MOISTURE, PORE |
| 알레르기 성분 | 자유 텍스트 | "향료, 알코올, 살리실산" |

알레르기 성분은 별도 성분 DB 없이 RAG 프롬프트에 직접 주입하여 LLM이 판단한다.

---

## 3. 핵심 기능

### 3-1. 회원 관리
- 이메일 / 비밀번호 회원가입 (bcrypt 암호화)
- 피부 타입, 피부 고민, 알레르기 성분 등록
- JWT 기반 로그인 / 로그아웃

### 3-2. 화장품 등록 (OCR)
- 앞면 + 뒷면 사진 2장 업로드
- Upstage Document Parse OCR로 텍스트 추출 (`ocr=force`)
- Solar LLM이 제품명 / 브랜드 / 성분 / confidence 파싱
- **confidence: high** → 자동 저장 + 벡터 동기화
- **confidence: low** → 파싱 결과 반환 → 사용자 확인/수정 후 저장 API 호출 (HITL)
- 이미지는 서버 로컬 `/uploads/`에 저장, DB에는 URL만 보관 (OCR 로그 용도)

### 3-3. 화장품 CRUD
- 수동 등록 / 조회 / 수정 / 삭제
- 삭제 시 벡터 동기화

### 3-4. 루틴 추천
- AM / PM 루틴 생성
- Solar LLM이 보유 화장품 중 적합한 제품과 사용 순서 추천
- 루틴 저장 / 조회 / 삭제

### 3-5. RAG 챗봇
- 질문 → Embedding → 벡터 유사도 검색 → 관련 화장품 3개 조회
- Solar LLM 호출 시 system 프롬프트에 피부 정보 + 관련 화장품 성분 주입 (ChatService 책임)
- 세션 기반 대화 관리: ChatSession 생성/삭제, 세션별 히스토리 유지
- 컨텍스트 윈도우: 세션 내 최근 10개 메시지만 LLM에 전달

---

## 4. 제외 기능

| 기능 | 제외 이유 |
|------|----------|
| 결제 (토스페이먼츠) | 낮은 우선순위, 향후 고도화 |
| 표준 성분 DB 매핑 | 외부 DB 의존, 프로젝트 방향과 모순 |
| 소셜 로그인 | 범위 초과 |

---

## 5. 데이터 모델

```
User
├── id (PK)
├── email (unique)
├── password (bcrypt)
├── nickname
├── skinType (Enum: DRY/OILY/COMBINATION/SENSITIVE/NORMAL)
├── skinConcerns (String: "ACNE,PORE,MOISTURE")
├── allergyIngredients (String: 자유 텍스트)
└── createdAt

Cosmetic  ← SOT + 벡터 임베딩 원본
├── id (PK)
├── userId (FK → User)
├── name
├── brand
├── category (Enum: 스킨/에센스/크림/선크림/클렌징/기타)
├── ingredients (전성분 텍스트)
├── imageUrl (로컬 파일 경로)
└── createdAt

Routine
├── id (PK)
├── userId (FK → User)
├── name
├── timeOfDay (Enum: AM/PM)
├── description (AI 생성)
└── createdAt

RoutineCosmetic  ← 중간 테이블
├── id (PK)
├── routineId (FK → Routine)
├── cosmeticId (FK → Cosmetic)
└── order (사용 순서)

ChatSession
├── id (UUID, PK)
├── userId (FK → User)
└── createdAt

ChatMessage
├── id (PK)
├── sessionId (FK → ChatSession)
├── role (Enum: USER/ASSISTANT)
├── content
└── createdAt

Vector Store  ← DB 테이블 없음
└── InMemory ConcurrentHashMap<Long, float[]>  (cosmeticId → embedding)
```

---

## 6. API 엔드포인트 (예정)

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/auth/signup` | 회원가입 |
| POST | `/auth/login` | 로그인 → JWT 반환 |
| GET | `/cosmetics` | 내 화장품 목록 |
| POST | `/cosmetics` | 수동 등록 |
| POST | `/cosmetics/ocr` | OCR 등록 (사진 업로드) |
| POST | `/cosmetics/ocr/confirm` | HITL 확인 후 저장 |
| PUT | `/cosmetics/{id}` | 수정 |
| DELETE | `/cosmetics/{id}` | 삭제 |
| GET | `/routines` | 루틴 목록 |
| POST | `/routines` | 루틴 생성 (AI 추천) |
| DELETE | `/routines/{id}` | 루틴 삭제 |
| POST | `/chat/sessions` | 세션 생성 |
| GET | `/chat/sessions` | 세션 목록 |
| DELETE | `/chat/sessions/{sessionId}` | 세션 삭제 |
| POST | `/chat/sessions/{sessionId}/messages` | 챗봇 질문 (RAG) |
| GET | `/chat/sessions/{sessionId}/messages` | 세션 히스토리 조회 |

---

## 7. 기술 스택 요약

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| ORM | Spring Data JPA |
| DB | MySQL 8 (Docker / RDS t3.micro) |
| 인증 | Spring Security + JWT (jjwt 0.12) |
| OCR | Upstage Document Parse |
| LLM | Upstage Solar (solar-pro3-260323) |
| Embedding | Upstage Embedding API (passage/query 분리) |
| Vector Store | InMemory → pgvector (전략 패턴) |
| HTTP Client | WebClient |
| Frontend | React + Vite (순수 CSS) |
| 프론트 배포 | Vercel |
| 백엔드 배포 | AWS EC2 t2.micro |

---

