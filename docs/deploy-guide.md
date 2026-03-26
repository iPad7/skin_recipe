# Skin Recipe — 배포 가이드

## 인프라 구성

```
사용자 브라우저
    │ HTTPS
    ▼
Vercel (React SPA)
    │ HTTPS :443
    ▼
EC2 t2.micro (Nginx → Spring Boot :8080)  ←── GitHub Actions (SSM, 포트 22 불필요)
    │ TCP :3306
    ▼
RDS t4g.micro (MySQL 8)
    │ HTTPS
    ▼
Upstage API (OCR / LLM / Embedding)
```

| 서비스 | 리전 | 스펙 | 비고 |
|--------|------|------|------|
| EC2 | ap-northeast-2 (서울) | t2.micro | 프리티어 |
| RDS | ap-northeast-2 (서울) | db.t4g.micro | 프리티어 |
| S3 | ap-northeast-2 (서울) | — | jar 전송용, 프리티어 |
| Vercel | — | — | GitHub 연동 자동 배포 |
| DuckDNS | — | — | 무료 도메인 (skin-recipe.duckdns.org) |

> **Vercel은 HTTPS 전용이므로 EC2 API도 반드시 HTTPS여야 함.** HTTP로 두면 Mixed Content 오류로 브라우저가 요청을 차단함. Nginx + DuckDNS + Let's Encrypt로 해결.

---

## 1. EC2 셋업

### 인스턴스 생성
- AMI: Ubuntu Server 22.04 LTS
- 인스턴스 유형: t2.micro
- 키 페어: Ed25519, `.pem` 로컬 보관 (SSH는 사용 안 하지만 비상용 보관)
- 스토리지: 8GB gp2

### 보안 그룹 인바운드 규칙

| 포트 | 프로토콜 | 소스 | 설명 |
|------|----------|------|------|
| 80 | TCP | 0.0.0.0/0 | HTTP (Let's Encrypt 인증 + HTTPS 리다이렉트) |
| 443 | TCP | 0.0.0.0/0 | HTTPS (Nginx) |

> SSH 22 포트는 열지 않음. GitHub Actions 배포는 AWS SSM으로 처리.
> Spring Boot 8080 포트는 외부에 열지 않음. Nginx가 내부에서 프록시.

### Elastic IP
탄력적 IP 할당 후 인스턴스에 연결. EC2 재시작 시 IP 고정 목적.

### Java 설치
EC2 접속(SSM Session Manager) 후:
```bash
sudo apt update && sudo apt install -y openjdk-21-jdk
java -version
```

### 앱 디렉토리 생성 및 권한 설정
```bash
sudo mkdir -p /home/ubuntu/app
sudo chown -R ubuntu:ubuntu /home/ubuntu/app
```

> `chown` 필수. ubuntu 유저가 app 하위에 uploads 디렉토리를 생성할 권한이 필요.

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
WorkingDirectory=/home/ubuntu/app
EnvironmentFile=/etc/skin-recipe/env
ExecStart=/usr/bin/java -jar /home/ubuntu/app/app.jar
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

> `WorkingDirectory` 필수. 없으면 `./uploads`가 `/uploads`(루트)로 해석되어 권한 오류 발생.

```bash
sudo systemctl daemon-reload
sudo systemctl enable skin-recipe
```

---

## 2. SSM 셋업

GitHub Actions가 포트 22 없이 EC2에 명령을 전달하기 위해 AWS Systems Manager를 사용.

### EC2 IAM 역할 생성
IAM → 역할 → 역할 생성
- 신뢰할 수 있는 엔터티: AWS 서비스 → EC2 Role for AWS Systems Manager
- 추가 정책: `AmazonS3ReadOnlyAccess` (S3에서 jar 다운로드용)
- 역할 이름: `skin-recipe-ec2-role`

EC2 인스턴스 → 작업 → 보안 → IAM 역할 수정 → `skin-recipe-ec2-role` 연결

### SSM Agent 설치 (Ubuntu 22.04는 기본 미설치)
```bash
sudo snap install amazon-ssm-agent --classic
sudo systemctl start snap.amazon-ssm-agent.amazon-ssm-agent.service
sudo systemctl enable snap.amazon-ssm-agent.amazon-ssm-agent.service
```

IAM 역할 연결 후 SSM Agent 재시작하면 Systems Manager → Fleet Manager에 인스턴스가 등록됨.

### AWS CLI v2 설치 (EC2에서 S3 다운로드에 필요)
SSM → Run Command → AWS-RunShellScript로 실행:
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "/tmp/awscliv2.zip" && unzip /tmp/awscliv2.zip -d /tmp && sudo /tmp/aws/install
```

> Ubuntu 22.04 apt 패키지에 `awscli`가 없음 → 공식 설치 방법 사용.
> `unzip`도 없으면: `sudo apt-get install -y unzip` 먼저 실행.

---

## 3. RDS 셋업

- 엔진: MySQL 8.0
- 템플릿: 프리티어
- 인스턴스 유형: db.t4g.micro
- 퍼블릭 액세스: 아니요
- EC2 컴퓨팅 리소스 연결: skin-recipe-server 선택 (보안 그룹 자동 구성)

### DB 및 유저 생성
EC2에서 mysql-client로 접속 후:
```sql
CREATE DATABASE skinrecipe CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'skinrecipe'@'%' IDENTIFIED BY '{비밀번호}';
GRANT ALL PRIVILEGES ON skinrecipe.* TO 'skinrecipe'@'%';
FLUSH PRIVILEGES;
```

---

## 4. S3 버킷 생성

- 버킷 이름: `skin-recipe-deploy` (글로벌 유일)
- 리전: ap-northeast-2 (서울)
- 퍼블릭 액세스: 모두 차단
- 나머지 기본값 유지

---

## 5. GitHub Actions

### IAM 유저 생성 (GitHub Actions용)
IAM → 사용자 → 사용자 생성
- 사용자 이름: `skin-recipe-github-actions`
- 정책: `AmazonSSMFullAccess`, `AmazonS3FullAccess`
- 액세스 키 생성 → 서드 파티 서비스 → 키/시크릿 복사 보관

### GitHub Secrets 등록
GitHub 레포 → Settings → Secrets and variables → Actions

| Secret 이름 | 값 |
|-------------|-----|
| `AWS_ACCESS_KEY_ID` | IAM 액세스 키 |
| `AWS_SECRET_ACCESS_KEY` | IAM 시크릿 키 |
| `AWS_REGION` | `ap-northeast-2` |
| `EC2_INSTANCE_ID` | EC2 인스턴스 ID (`i-xxxxxxxxx`) |
| `S3_BUCKET` | `skin-recipe-deploy` |

### 워크플로우
`.github/workflows/deploy.yml` 참고. `main` 브랜치 push 시 자동 실행:
1. Gradle `bootJar` 빌드
2. AWS 자격증명 설정
3. jar → S3 업로드
4. SSM SendCommand → EC2에서 S3 다운로드 → systemd 재시작

---

## 6. HTTPS — DuckDNS + Nginx + Let's Encrypt

Vercel(HTTPS)에서 HTTP EC2 API를 호출하면 Mixed Content 오류로 브라우저가 차단함. 무료 도메인 + Nginx 리버스 프록시 + Let's Encrypt SSL로 해결.

### DuckDNS 도메인 등록
1. https://www.duckdns.org 접속 → GitHub/Google 로그인
2. subdomain 입력 → **add domain**
3. current ip에 EC2 Elastic IP 입력 → **update ip**

### Nginx + certbot 설치
SSM Session Manager로 EC2 접속 후 root 전환(`sudo -i`):
```bash
apt update && apt install -y nginx certbot python3-certbot-nginx
```

### SSL 인증서 발급
> Let's Encrypt가 도메인 소유 확인을 위해 포트 80으로 접근하므로, 보안 그룹에 80이 열려 있어야 함.

```bash
certbot --nginx -d skin-recipe.duckdns.org
```

이메일 입력 → 약관 동의(Y) → 뉴스레터(N)

### Nginx 프록시 설정
```bash
cat > /etc/nginx/sites-enabled/default << 'EOF'
server {
    listen 80;
    listen [::]:80;
    server_name skin-recipe.duckdns.org;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    listen [::]:443 ssl;
    server_name skin-recipe.duckdns.org;

    ssl_certificate /etc/letsencrypt/live/skin-recipe.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/skin-recipe.duckdns.org/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF

nginx -t && systemctl reload nginx
```

---

## 7. Vercel (프론트엔드)

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
| `VITE_API_BASE_URL` | `https://skin-recipe.duckdns.org` |

> CORS 설정도 Vercel 도메인으로 업데이트 필요 (`SecurityConfig.java`)

---

## 8. 비용 관리 주의사항

- Elastic IP는 인스턴스 **중지 시** 과금 → 항시 가동 유지
- RDS는 중지 후 7일 뒤 자동 재시작 → 그냥 켜두는 게 나음
- 프리티어는 계정 생성 후 12개월까지만 적용
- 스냅샷/AMI 생성 시 스토리지 비용 발생
- 프리티어 750시간은 전 리전 합산 → 여러 인스턴스 동시 실행 주의

---

## 9. 보안 주의사항

- EC2 인바운드: 80(HTTP), 443(HTTPS)만 개방. SSH 22 불필요 — SSM으로 대체
- Spring Boot 8080은 외부에 열지 않음 — Nginx가 내부에서만 접근
- RDS 3306은 EC2 보안 그룹에서만 인바운드 허용
- GitHub Secrets에만 키 저장, 레포에 절대 커밋 금지
- `/etc/skin-recipe/env` 권한: `chmod 600`
- `.pem` 파일 권한: `chmod 400`, `.gitignore` 등록 확인
