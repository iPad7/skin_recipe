# Skin Recipe — 배포 가이드

## 인프라 구성

```
사용자 브라우저
    │ HTTPS
    ▼
Vercel (React SPA)
    │ HTTP :8080
    ▼
EC2 t2.micro (Spring Boot)  ←── GitHub Actions (SSH :22)
    │ TCP :3306
    ▼
RDS t3.micro (MySQL 8)
    │ HTTPS
    ▼
Upstage API (OCR / LLM / Embedding)
```

| 서비스 | 리전 | 스펙 | 비고 |
|--------|------|------|------|
| EC2 | ap-northeast-2 (서울) | t2.micro | 프리티어 |
| RDS | ap-northeast-2 (서울) | db.t4g.micro | 프리티어 |
| Vercel | — | — | GitHub 연동 자동 배포 |

---

## 1. EC2 셋업

### 인스턴스 생성
- AMI: Ubuntu Server 22.04 LTS
- 인스턴스 유형: t2.micro
- 키 페어: Ed25519, `.pem` 로컬 보관
- 스토리지: 8GB gp2

### 보안 그룹 인바운드 규칙

| 포트 | 프로토콜 | 소스 | 설명 |
|------|----------|------|------|
| 22 | TCP | 내 IP | Local SSH access |
| 8080 | TCP | 0.0.0.0/0 | Spring Boot API |

### Elastic IP
탄력적 IP 할당 후 인스턴스에 연결. EC2 재시작 시 IP 고정 목적.

### Java 설치
```bash
sudo apt update && sudo apt install -y openjdk-21-jdk
java -version
```

### 앱 디렉토리 생성
```bash
sudo mkdir -p /home/ubuntu/app
```

### 환경변수 파일
```bash
sudo mkdir -p /etc/skin-recipe
sudo nano /etc/skin-recipe/env
sudo chmod 600 /etc/skin-recipe/env
```

`/etc/skin-recipe/env` 내용:
```
DB_URL=jdbc:mysql://{RDS 엔드포인트}:3306/skinrecipe?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
DB_USERNAME=skinrecipe
DB_PASSWORD={DB 비밀번호}
JWT_SECRET={openssl rand -base64 32 결과}
UPSTAGE_API_KEY={Upstage API 키}
```

### systemd 서비스 등록
```bash
sudo nano /etc/systemd/system/skin-recipe.service
```

```ini
[Unit]
Description=Skin Recipe Spring Boot App
After=network.target

[Service]
User=ubuntu
EnvironmentFile=/etc/skin-recipe/env
ExecStart=/usr/bin/java -jar /home/ubuntu/app/app.jar
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable skin-recipe
```

---

## 2. RDS 셋업

- 엔진: MySQL 8.0
- 템플릿: 프리티어
- 인스턴스 유형: db.t4g.micro
- 퍼블릭 액세스: 아니요
- EC2 컴퓨팅 리소스 연결: skin-recipe-server 선택 (보안 그룹 자동 구성)

### DB 및 유저 생성
```sql
CREATE DATABASE skinrecipe CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'skinrecipe'@'%' IDENTIFIED BY '{비밀번호}';
GRANT ALL PRIVILEGES ON skinrecipe.* TO 'skinrecipe'@'%';
FLUSH PRIVILEGES;
```

---

## 3. GitHub Actions

### GitHub Secrets 등록
GitHub 레포 → Settings → Secrets and variables → Actions

| Secret 이름 | 값 |
|-------------|-----|
| `EC2_HOST` | Elastic IP |
| `EC2_USER` | `ubuntu` |
| `EC2_SSH_KEY` | `.pem` 파일 전체 내용 |

### 워크플로우
`.github/workflows/deploy.yml` 참고. `main` 브랜치 push 시 자동 실행:
1. Gradle `bootJar` 빌드
2. SCP로 jar 전송
3. SSH 접속 → systemd 재시작

---

## 4. Vercel (프론트엔드)

GitHub 레포 연결 후 설정:

| 항목 | 값 |
|------|-----|
| Root Directory | `frontend` |
| Build Command | `npm run build` |
| Output Directory | `dist` |

### 환경변수
Vercel 대시보드 → Settings → Environment Variables:

| 키 | 값 |
|----|-----|
| `VITE_API_BASE_URL` | `http://{EC2 Elastic IP}:8080` |

> CORS 설정도 Vercel 도메인으로 업데이트 필요 (`SecurityConfig.java`)

---

## 5. 비용 관리 주의사항

- Elastic IP는 인스턴스 **중지 시** 과금 → 항시 가동 유지
- RDS는 중지 후 7일 뒤 자동 재시작 → 그냥 켜두는 게 나음
- 프리티어는 계정 생성 후 12개월까지만 적용
- 스냅샷/AMI 생성 시 스토리지 비용 발생
- 프리티어 750시간은 전 리전 합산 → 여러 인스턴스 동시 실행 주의

---

## 6. 보안 주의사항

- `.pem` 파일 권한: `chmod 400`
- `.pem` 파일 및 `application-local.yml` `.gitignore` 등록 확인
- GitHub Secrets에만 키 저장, 레포에 절대 커밋 금지
- SSH 22 포트는 내 IP만 허용 (GitHub Actions IP는 0.0.0.0/0 필요)
- RDS 3306은 EC2 보안 그룹에서만 인바운드 허용
