# Skin Recipe — 테스트 가이드

## 테스트 실행 방법

```bash
# 전체 테스트 실행
./gradlew test

# 특정 클래스만 실행
./gradlew test --tests "com.mycosmetic.security.JwtUtilTest"
./gradlew test --tests "com.mycosmetic.service.AuthServiceTest"
./gradlew test --tests "com.mycosmetic.controller.AuthControllerTest"

# 캐시 무시하고 강제 재실행
./gradlew test --rerun

# HTML 리포트 확인 (브라우저)
open build/reports/tests/test/index.html
```

---

## 테스트 클래스 목록

---

### `JwtUtilTest`
**위치:** `src/test/java/com/mycosmetic/security/JwtUtilTest.java`
**종류:** 순수 단위 테스트 (Spring 컨텍스트 없음)
**목적:** JWT 토큰 생성 / 검증 / 파싱 로직이 올바르게 동작하는지 확인

| 테스트명 | 검증 내용 |
|----------|-----------|
| 토큰 생성 후 이메일 추출이 일치해야 한다 | `generateToken(email)` → `getEmailFromToken()` 결과가 원본 이메일과 동일한지 |
| 유효한 토큰은 검증을 통과해야 한다 | 정상 발급된 토큰이 `validateToken()` = `true` 반환하는지 |
| 위변조된 토큰은 검증에 실패해야 한다 | 토큰 뒤에 임의 문자열 추가 시 `validateToken()` = `false` 반환하는지 |
| 만료된 토큰은 검증에 실패해야 한다 | `expiration = -1`로 생성한 토큰이 `validateToken()` = `false` 반환하는지 |

---

### `AuthServiceTest`
**위치:** `src/test/java/com/mycosmetic/service/AuthServiceTest.java`
**종류:** 단위 테스트 (Mockito로 Repository / PasswordEncoder / JwtUtil Mock 처리)
**목적:** 회원가입 / 로그인 비즈니스 로직이 올바르게 동작하는지 확인

| 테스트명 | 검증 내용 |
|----------|-----------|
| 정상적인 회원가입 요청이면 User가 저장된다 | `userRepository.save()` 가 1회 호출되는지 |
| 이미 존재하는 이메일이면 예외가 발생한다 | `existsByEmail = true` 일 때 `IllegalArgumentException` 발생 + `save()` 미호출 확인 |
| 올바른 이메일/비밀번호이면 JWT가 반환된다 | 로그인 성공 시 `accessToken` 필드가 포함된 `LoginResponse` 반환 확인 |
| 존재하지 않는 이메일이면 예외가 발생한다 | `findByEmail = empty` 일 때 `IllegalArgumentException` 발생 확인 |
| 비밀번호가 틀리면 예외가 발생한다 | `passwordEncoder.matches = false` 일 때 `IllegalArgumentException` 발생 확인 |

---

### `AuthControllerTest`
**위치:** `src/test/java/com/mycosmetic/controller/AuthControllerTest.java`
**종류:** 슬라이스 테스트 (`@WebMvcTest` — HTTP 계층만 로드, `AuthService` Mock 처리)
**목적:** HTTP 요청/응답 형식, 상태 코드, Validation이 올바르게 동작하는지 확인

| 테스트명 | 검증 내용 |
|----------|-----------|
| 유효한 회원가입 요청이면 200과 메시지를 반환한다 | 정상 요청 시 `200 OK` + `{ "message": "회원가입이 완료되었습니다." }` 반환 확인 |
| 이메일 형식이 잘못되면 400을 반환한다 | `email: "not-an-email"` 전송 시 `@Email` Validation이 `400 Bad Request` 반환 확인 |
| 중복 이메일이면 400을 반환한다 | `AuthService`가 `IllegalArgumentException` 던질 때 `GlobalExceptionHandler`가 `400` 변환하는지 확인 |
| 올바른 로그인 요청이면 200과 accessToken을 반환한다 | 정상 로그인 시 `200 OK` + `{ "accessToken": "..." }` 반환 확인 |
| 잘못된 자격증명이면 400을 반환한다 | `AuthService`가 `IllegalArgumentException` 던질 때 `400 Bad Request` 반환 확인 |

---

### `SkinRecipeApplicationTests`
**위치:** `src/test/java/com/mycosmetic/SkinRecipeApplicationTests.java`
**종류:** 통합 테스트 (`@SpringBootTest` — 전체 컨텍스트 로드)
**목적:** 애플리케이션 컨텍스트가 에러 없이 정상 기동되는지 확인
**전제 조건:** MySQL Docker 컨테이너가 실행 중이어야 합니다

| 테스트명 | 검증 내용 |
|----------|-----------|
| contextLoads() | Spring 전체 컨텍스트 (DB 연결 포함) 기동 성공 여부 |

---

## 테스트 결과 로그

### Phase 1 — 2026-03-24

```
테스트 환경: Java 21, Spring Boot 3.5, MySQL 8 (Docker)

com.mycosmetic.security.JwtUtilTest
  ✅ 토큰 생성 후 이메일 추출이 일치해야 한다       (0.159s)
  ✅ 유효한 토큰은 검증을 통과해야 한다             (0.003s)
  ✅ 만료된 토큰은 검증에 실패해야 한다             (0.004s)
  ✅ 위변조된 토큰은 검증에 실패해야 한다           (0.003s)

com.mycosmetic.service.AuthServiceTest
  ✅ 정상적인 회원가입 요청이면 User가 저장된다      (0.006s)
  ✅ 이미 존재하는 이메일이면 예외가 발생한다        (0.312s)
  ✅ 올바른 이메일/비밀번호이면 JWT가 반환된다       (0.008s)
  ✅ 존재하지 않는 이메일이면 예외가 발생한다        (0.003s)
  ✅ 비밀번호가 틀리면 예외가 발생한다              (0.004s)

com.mycosmetic.controller.AuthControllerTest
  ✅ 유효한 회원가입 요청이면 200과 메시지를 반환한다 (0.011s)
  ✅ 이메일 형식이 잘못되면 400을 반환한다           (0.031s)
  ✅ 중복 이메일이면 400을 반환한다                 (0.287s)
  ✅ 올바른 로그인 요청이면 200과 accessToken을 반환한다 (0.056s)
  ✅ 잘못된 자격증명이면 400을 반환한다             (0.013s)

com.mycosmetic.SkinRecipeApplicationTests
  ✅ contextLoads()                               (0.417s)

결과: 15/15 통과  |  실패: 0  |  소요 시간: ~12s
```

> **비고:** 초기 실행 시 `GlobalExceptionHandler` 미등록으로 인해 `AuthControllerTest` 2개 케이스 실패 (500 반환).
> `GlobalExceptionHandler` 추가 후 전체 통과.
