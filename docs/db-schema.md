# Skin Recipe — DB 스키마 정의서

> 코드 기준: `src/main/java/com/mycosmetic/domain/` 엔티티 클래스 직접 분석
>
> JPA DDL 전략: `ddl-auto: update`

---

## 테이블 목록

| 테이블명 | 엔티티 클래스 | 설명 |
|----------|--------------|------|
| `users` | `User` | 회원 정보 |
| `cosmetics` | `Cosmetic` | 화장품 정보 |
| `routines` | `Routine` | 루틴 정보 |
| `routine_cosmetics` | `RoutineCosmetic` | 루틴-화장품 중간 테이블 |
| `chat_sessions` | `ChatSession` | 채팅 세션 |
| `chat_messages` | `ChatMessage` | 채팅 메시지 |

---

## 1. `users`

| 컬럼명 | 타입 | NULL | 기본값 | 제약 | 설명 |
|--------|------|------|--------|------|------|
| `id` | BIGINT | NN | AUTO_INCREMENT | PK | |
| `email` | VARCHAR(255) | NN | - | UNIQUE | 로그인 이메일 |
| `password` | VARCHAR(255) | NN | - | | BCrypt 해시 |
| `nickname` | VARCHAR(255) | NN | - | | 표시 이름 |
| `skin_type` | VARCHAR(255) | NN | - | | Enum 문자열: `DRY / OILY / COMBINATION / SENSITIVE / NORMAL` |
| `skin_concerns` | VARCHAR(255) | NULL | NULL | | 콤마 구분 문자열. 예: `"ACNE,PORE"` |
| `allergy_ingredients` | VARCHAR(255) | NULL | NULL | | 자유 텍스트. 예: `"향료, 알코올"` |
| `created_at` | DATETIME(6) | NN | - | updatable=false | `@CreationTimestamp` 자동 설정 |

**JPA 설정**
- PK 전략: `GenerationType.IDENTITY`
- `skin_type`: `@Enumerated(EnumType.STRING)` — 문자열로 저장
- `created_at`: `@CreationTimestamp`, `updatable = false`

---

## 2. `cosmetics`

| 컬럼명 | 타입 | NULL | 기본값 | 제약 | 설명 |
|--------|------|------|--------|------|------|
| `id` | BIGINT | NN | AUTO_INCREMENT | PK | |
| `user_id` | BIGINT | NN | - | FK → `users.id` | 소유자 |
| `name` | VARCHAR(255) | NN | - | | 제품명 |
| `brand` | VARCHAR(255) | NULL | NULL | | 브랜드명 |
| `category` | VARCHAR(255) | NN | - | | Enum 문자열: `SKIN / ESSENCE / CREAM / SUNSCREEN / CLEANSING / ETC` |
| `ingredients` | TEXT | NULL | NULL | | 전성분 원문 |
| `image_url` | VARCHAR(255) | NULL | NULL | | 업로드 이미지 경로. 예: `/uploads/abc.jpg` |
| `created_at` | DATETIME(6) | NN | - | updatable=false | `@CreationTimestamp` 자동 설정 |

**JPA 설정**
- PK 전략: `GenerationType.IDENTITY`
- `user_id`: `@ManyToOne(fetch = FetchType.LAZY)` — 지연 로딩
- `category`: `@Enumerated(EnumType.STRING)`
- `ingredients`: `@Column(columnDefinition = "TEXT")`
- FK 삭제 정책: JPA 레벨 Cascade 없음 — `users` 삭제 시 `AuthService.deleteAccount()`에서 애플리케이션 레벨로 먼저 삭제

---

## 3. `routines`

| 컬럼명 | 타입 | NULL | 기본값 | 제약 | 설명 |
|--------|------|------|--------|------|------|
| `id` | BIGINT | NN | AUTO_INCREMENT | PK | |
| `user_id` | BIGINT | NN | - | FK → `users.id` | 소유자 |
| `name` | VARCHAR(255) | NN | - | | 루틴 이름. 예: `"아침 루틴"` |
| `time_of_day` | VARCHAR(255) | NN | - | | Enum 문자열: `AM / PM` |
| `description` | TEXT | NULL | NULL | | AI 생성 루틴 설명 |
| `created_at` | DATETIME(6) | NN | - | updatable=false | `@CreationTimestamp` 자동 설정 |

**JPA 설정**
- PK 전략: `GenerationType.IDENTITY`
- `user_id`: `@ManyToOne(fetch = FetchType.LAZY)`
- `time_of_day`: `@Enumerated(EnumType.STRING)`
- `description`: `@Column(columnDefinition = "TEXT")`
- `routine_cosmetics` 연관: `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` — 루틴 삭제 시 하위 `routine_cosmetics` 자동 삭제
- 정렬: `@OrderBy("order ASC")`

---

## 4. `routine_cosmetics`

| 컬럼명 | 타입 | NULL | 기본값 | 제약 | 설명 |
|--------|------|------|--------|------|------|
| `id` | BIGINT | NN | AUTO_INCREMENT | PK | |
| `routine_id` | BIGINT | NN | - | FK → `routines.id` | 루틴 참조 |
| `cosmetic_id` | BIGINT | NN | - | FK → `cosmetics.id` | 화장품 참조 |
| `order` | INT | NN | - | | 루틴 내 사용 순서 (1부터 시작) |

**JPA 설정**
- PK 전략: `GenerationType.IDENTITY`
- `routine_id`: `@ManyToOne(fetch = FetchType.LAZY)`
- `cosmetic_id`: `@ManyToOne(fetch = FetchType.LAZY)`
- `order`: 예약어 충돌 방지 — `@Column(name = "` `` `order` `` `")`로 백틱 처리
- **삭제 정책**: `cosmetic_id` 참조 화장품은 삭제 불가 — `CosmeticService.delete()`에서 `RoutineCosmeticRepository.existsByCosmeticId()` 사전 검증 후 400 반환

---

## 5. `chat_sessions`

| 컬럼명 | 타입 | NULL | 기본값 | 제약 | 설명 |
|--------|------|------|--------|------|------|
| `id` | BINARY(16) / UUID | NN | UUID 자동 생성 | PK | 세션 ID 추측 불가 목적 |
| `user_id` | BIGINT | NN | - | FK → `users.id` | 소유자 |
| `created_at` | DATETIME(6) | NN | - | updatable=false | `@CreationTimestamp` 자동 설정 |

**JPA 설정**
- PK 전략: `GenerationType.UUID` — Hibernate가 UUID 자동 생성
- `user_id`: `@ManyToOne(fetch = FetchType.LAZY)`
- `messages` 연관: `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` — 세션 삭제 시 하위 `chat_messages` 자동 삭제
- 정렬: `@OrderBy("createdAt ASC")`

---

## 6. `chat_messages`

| 컬럼명 | 타입 | NULL | 기본값 | 제약 | 설명 |
|--------|------|------|--------|------|------|
| `id` | BIGINT | NN | AUTO_INCREMENT | PK | |
| `session_id` | BINARY(16) / UUID | NN | - | FK → `chat_sessions.id` | 세션 참조 |
| `role` | VARCHAR(255) | NN | - | | Enum 문자열: `USER / ASSISTANT` |
| `content` | TEXT | NN | - | | 메시지 내용 |
| `created_at` | DATETIME(6) | NN | - | updatable=false | `@CreationTimestamp` 자동 설정 |

**JPA 설정**
- PK 전략: `GenerationType.IDENTITY`
- `session_id`: `@ManyToOne(fetch = FetchType.LAZY)`
- `role`: `@Enumerated(EnumType.STRING)`
- `content`: `@Column(nullable = false, columnDefinition = "TEXT")`

---

## 연관관계 요약

| 관계 | 방향 | Cascade | orphanRemoval | Fetch | 비고 |
|------|------|---------|---------------|-------|------|
| User → Cosmetic | 1:N | 없음 | 없음 | LAZY | 애플리케이션 레벨 삭제 |
| User → Routine | 1:N | 없음 | 없음 | LAZY | 애플리케이션 레벨 삭제 |
| User → ChatSession | 1:N | 없음 | 없음 | LAZY | 애플리케이션 레벨 삭제 |
| Routine → RoutineCosmetic | 1:N | ALL | ✅ | LAZY | 루틴 삭제 시 자동 삭제 |
| Cosmetic → RoutineCosmetic | 1:N | 없음 | 없음 | LAZY | 삭제 불가 (400 반환) |
| ChatSession → ChatMessage | 1:N | ALL | ✅ | LAZY | 세션 삭제 시 자동 삭제 |

---

## 회원 탈퇴 시 삭제 순서

`AuthService.deleteAccount()`에서 FK 제약 위반 없이 삭제하기 위해 아래 순서로 처리:

1. `chat_messages` — `ChatSession` cascade로 자동 삭제
2. `chat_sessions`
3. `routine_cosmetics` — `Routine` cascade로 자동 삭제
4. `routines`
5. `cosmetics` + 벡터 스토어 제거
6. `users`

---

## 인덱스

JPA `ddl-auto: update` 기준으로 자동 생성되는 인덱스:

| 테이블 | 컬럼 | 인덱스 종류 | 생성 이유 |
|--------|------|------------|----------|
| `users` | `email` | UNIQUE INDEX | `@Column(unique = true)` |
| `cosmetics` | `user_id` | INDEX | FK 자동 생성 |
| `routines` | `user_id` | INDEX | FK 자동 생성 |
| `routine_cosmetics` | `routine_id` | INDEX | FK 자동 생성 |
| `routine_cosmetics` | `cosmetic_id` | INDEX | FK 자동 생성 |
| `chat_sessions` | `user_id` | INDEX | FK 자동 생성 |
| `chat_messages` | `session_id` | INDEX | FK 자동 생성 |

> 별도 `@Index` 어노테이션은 현재 적용되지 않음. 성능 최적화 시 `chat_messages.created_at`, `chat_sessions.created_at` 인덱스 추가 고려.

---

## 벡터 스토어 (DB 외)

| 항목 | 내용 |
|------|------|
| 저장소 | `InMemoryVectorStoreService` — `ConcurrentHashMap<Long, float[]>` |
| 키 | `cosmeticId` (Cosmetic PK) |
| 값 | Upstage Embedding API 응답 `float[]` |
| 초기화 | 앱 기동 시 `@EventListener(ApplicationReadyEvent)` — DB 전체 화장품 로드 |
| 동기화 | 화장품 저장/삭제 시 실시간 갱신 |
| 영속성 | 없음 — 재시작 시 DB에서 재로드 |
| 향후 계획 | `PgVectorStoreService`로 교체 (전략 패턴으로 설계) |

---

## DBML (dbdiagram.io용)

```
Table user {
  id bigint [pk, increment]
  email varchar [unique, not null]
  password varchar [not null]
  nickname varchar [not null]
  skin_type varchar [not null, note: 'DRY / OILY / COMBINATION / SENSITIVE / NORMAL']
  skin_concerns varchar [note: '콤마 구분 문자열. 예: ACNE,MOISTURE']
  allergy_ingredients varchar [note: '자유 텍스트. 예: 향료, 알코올']
  created_at datetime [not null]
}

Table cosmetic {
  id bigint [pk, increment]
  user_id bigint [not null, ref: > user.id]
  name varchar [not null]
  brand varchar
  category varchar [note: 'SKIN / ESSENCE / CREAM / SUNSCREEN / CLEANSING / ETC']
  ingredients text
  image_url varchar
  created_at datetime [not null]
}

Table routine {
  id bigint [pk, increment]
  user_id bigint [not null, ref: > user.id]
  name varchar [not null]
  time_of_day varchar [not null, note: 'AM / PM']
  description text
  created_at datetime [not null]
}

Table routine_cosmetic {
  id bigint [pk, increment]
  routine_id bigint [not null, ref: > routine.id]
  cosmetic_id bigint [not null, ref: > cosmetic.id]
  order int [not null]
}

Table chat_session {
  id uuid [pk]
  user_id bigint [not null, ref: > user.id]
  created_at datetime [not null]
}

Table chat_message {
  id bigint [pk, increment]
  session_id uuid [not null, ref: > chat_session.id]
  role varchar [not null, note: 'USER / ASSISTANT']
  content text [not null]
  created_at datetime [not null]
}
```
