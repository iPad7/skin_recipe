# Skin Recipe — API 정의서

## 공통 사항

### Base URL
```
http://localhost:8080
```

### 인증
JWT Bearer 토큰 방식. 로그인 후 발급된 `accessToken`을 모든 인증 필요 요청의 헤더에 포함.

```
Authorization: Bearer <accessToken>
```

인증이 필요한 엔드포인트에서 토큰이 없거나 유효하지 않으면 `401 Unauthorized` 반환.

---

### 에러 응답 포맷

모든 에러는 아래 형식으로 반환됩니다.

```json
{
  "message": "에러 설명 메시지"
}
```

| 상태코드 | 발생 조건 |
|---------|----------|
| `400 Bad Request` | 입력값 검증 실패, 중복 이메일, 잘못된 요청 |
| `401 Unauthorized` | JWT 없음 또는 유효하지 않은 토큰 |
| `404 Not Found` | 존재하지 않는 리소스 (사용자, 세션 등) |
| `500 Internal Server Error` | 서버 내부 오류 |

---

## 1. 인증 — `/auth`

### `POST /auth/signup` — 회원가입

**인증 불필요**

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "홍길동",
  "skinType": "NORMAL",
  "skinConcerns": "ACNE,MOISTURE",
  "allergyIngredients": "향료, 알코올"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | ✅ | 이메일 형식 |
| password | String | ✅ | |
| nickname | String | ✅ | |
| skinType | String | ✅ | `DRY` / `OILY` / `COMBINATION` / `SENSITIVE` / `NORMAL` |
| skinConcerns | String | ❌ | 콤마 구분. 예: `ACNE,MOISTURE,PORE` |
| allergyIngredients | String | ❌ | 자유 텍스트. 예: `향료, 알코올` |

**Response `200 OK`**
```json
{
  "message": "회원가입이 완료되었습니다."
}
```

**에러**
- `400` — 이메일 형식 오류, 필수 필드 누락, 중복 이메일

---

### `POST /auth/login` — 로그인

**인증 불필요**

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response `200 OK`**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**에러**
- `400` — 존재하지 않는 이메일, 비밀번호 불일치

---

### `GET /auth/me` — 내 정보 조회

**인증 필요**

**Response `200 OK`**
```json
{
  "email": "user@example.com",
  "nickname": "홍길동",
  "skinType": "DRY",
  "skinConcerns": "ACNE,PORE",
  "allergyIngredients": "향료, 알코올"
}
```

---

### `PUT /auth/me` — 내 정보 수정

**인증 필요**

**Request Body**
```json
{
  "nickname": "홍길동",
  "skinType": "OILY",
  "skinConcerns": "PORE,MOISTURE",
  "allergyIngredients": "향료"
}
```

**Response `200 OK`** — 수정된 사용자 정보 (`GET /auth/me` 응답과 동일)

**에러**
- `400` — nickname 또는 skinType 누락

---

### `DELETE /auth/me` — 회원 탈퇴

**인증 필요**

**Response `200 OK`**
```json
{
  "message": "회원 탈퇴가 완료되었습니다."
}
```

**처리 순서:** 채팅 세션·메시지 → 루틴·루틴 화장품 → 화장품·벡터 → 회원 순서로 삭제 (기존 cascade 활용)

**에러**
- `400` — 존재하지 않는 사용자

---

## 2. 화장품 — `/cosmetics`

모든 엔드포인트 **인증 필요**.

### `GET /cosmetics` — 내 화장품 목록 조회

**Response `200 OK`**
```json
[
  {
    "id": 1,
    "name": "Heartleaf Calming Toner Skin Booster",
    "brand": "Abib",
    "category": "SKIN",
    "ingredients": "정제수, 호투냐니아 코르다타 추출물, ...",
    "imageUrl": "/uploads/abc123.jpg",
    "createdAt": "2026-03-25T15:34:44Z"
  }
]
```

---

### `POST /cosmetics` — 수동 등록

**Request Body**
```json
{
  "name": "Heartleaf Calming Toner",
  "brand": "Abib",
  "category": "SKIN",
  "ingredients": "정제수, ..."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| name | String | ✅ | |
| brand | String | ❌ | |
| category | String | ❌ | `SKIN` / `ESSENCE` / `CREAM` / `SUNSCREEN` / `CLEANSING` / `ETC` |
| ingredients | String | ❌ | 전성분 텍스트 |

**Response `200 OK`** — 등록된 화장품 객체 (목록 조회 응답과 동일한 구조)

**에러**
- `400` — name 누락

---

### `PUT /cosmetics/{id}` — 수정

**Request Body** — `POST /cosmetics`와 동일 구조

**Response `200 OK`** — 수정된 화장품 객체

**에러**
- `400` — 소유자 불일치

---

### `DELETE /cosmetics/{id}` — 삭제

**Response `204 No Content`**

**에러**
- `400` — 소유자 불일치

---

### `POST /cosmetics/ocr` — OCR 등록

**Content-Type:** `multipart/form-data`

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| frontImage | File | ✅ | 화장품 앞면 이미지 |
| backImage | File | ✅ | 화장품 뒷면 이미지 (성분표) |

**Response `200 OK` — confidence: high (자동 저장)**
```json
{
  "id": 3,
  "name": "R.E.D BLEMISH For Men Calming All In One",
  "brand": "Dr.G",
  "category": "CREAM",
  "ingredients": "정제수, ...",
  "imageUrl": "/uploads/xyz.jpg",
  "createdAt": "2026-03-25T15:50:00Z"
}
```

**Response `200 OK` — confidence: low (HITL 대기)**
```json
{
  "name": "R.E.D BLEMISH For Men",
  "brand": "Dr.G",
  "category": "CREAM",
  "ingredients": "정제수, ...",
  "imageUrl": "/uploads/xyz.jpg",
  "confidence": "low"
}
```

**에러**
- `500` — OCR 또는 LLM 호출 실패

---

### `POST /cosmetics/ocr/confirm` — HITL 확인 저장

**Request Body**
```json
{
  "name": "R.E.D BLEMISH For Men Calming All In One",
  "brand": "Dr.G",
  "category": "CREAM",
  "ingredients": "정제수, ...",
  "imageUrl": "/uploads/xyz.jpg"
}
```

**Response `200 OK`** — 저장된 화장품 객체 (목록 조회 응답과 동일한 구조)

---

## 3. 루틴 — `/routines`

모든 엔드포인트 **인증 필요**.

### `POST /routines` — 루틴 생성 (AI 추천)

**Request Body**
```json
{
  "timeOfDay": "AM"
}
```

| 필드 | 값 |
|------|----|
| timeOfDay | `AM` / `PM` |

**Response `200 OK`**
```json
{
  "id": 1,
  "name": "아침 루틴",
  "timeOfDay": "AM",
  "description": "수분 공급과 자외선 차단에 집중한 루틴입니다.",
  "steps": [
    {
      "order": 1,
      "cosmeticId": 2,
      "cosmeticName": "Heartleaf Calming Toner Skin Booster",
      "brand": "Abib"
    },
    {
      "order": 2,
      "cosmeticId": 1,
      "cosmeticName": "R.E.D BLEMISH For Men Calming All In One",
      "brand": "Dr.G"
    }
  ],
  "createdAt": "2026-03-25T16:00:00Z"
}
```

**에러**
- `400` — 보유 화장품 없음

---

### `GET /routines` — 루틴 목록 조회

**Response `200 OK`** — 루틴 객체 배열 (생성 응답과 동일한 구조)

---

### `DELETE /routines/{id}` — 루틴 삭제

**Response `204 No Content`**

**에러**
- `400` — 소유자 불일치 또는 존재하지 않는 루틴

---

## 4. 챗봇 — `/chat/sessions`

모든 엔드포인트 **인증 필요**.

### `POST /chat/sessions` — 세션 생성

**Request Body** — 없음

**Response `200 OK`**
```json
{
  "id": "04f22059-f407-4b26-aef4-2f297e51297a",
  "createdAt": "2026-03-25T15:34:44Z"
}
```

---

### `GET /chat/sessions` — 세션 목록 조회

**Response `200 OK`** — 최신순 정렬
```json
[
  {
    "id": "04f22059-f407-4b26-aef4-2f297e51297a",
    "createdAt": "2026-03-25T15:34:44Z"
  }
]
```

---

### `DELETE /chat/sessions/{sessionId}` — 세션 삭제

세션 삭제 시 하위 메시지 전체 cascade 삭제.

**Response `204 No Content`**

**에러**
- `400` — 소유자 불일치
- `404` — 존재하지 않는 세션

---

### `POST /chat/sessions/{sessionId}/messages` — 챗봇 질문 (RAG)

**Request Body**
```json
{
  "message": "내 피부에 맞는 루틴 알려줘"
}
```

| 필드 | 타입 | 필수 |
|------|------|------|
| message | String | ✅ (빈 문자열 불가) |

**Response `200 OK`**
```json
{
  "answer": "보유하신 제품 중에서 ..."
}
```

**RAG 파이프라인 (내부 처리 순서):**
1. 질문 → Embedding API (query 모델) → 벡터화
2. 인메모리 벡터 검색 (topK=5)
3. 관련 화장품 상세 + 사용자 피부 정보 조회
4. 세션 내 최근 10개 메시지 히스토리 로드
5. System 프롬프트 조립 → Solar LLM 호출
6. USER + ASSISTANT 메시지 저장

**에러**
- `400` — message 빈 문자열, 소유자 불일치
- `404` — 존재하지 않는 세션

---

### `GET /chat/sessions/{sessionId}/messages` — 히스토리 조회

**Response `200 OK`** — 시간순 정렬
```json
[
  {
    "id": 1,
    "role": "USER",
    "content": "내 피부에 맞는 루틴 알려줘",
    "createdAt": "2026-03-25T16:02:48Z"
  },
  {
    "id": 2,
    "role": "ASSISTANT",
    "content": "보유하신 제품 중에서 ...",
    "createdAt": "2026-03-25T16:02:48Z"
  }
]
```

**에러**
- `400` — 소유자 불일치
- `404` — 존재하지 않는 세션
