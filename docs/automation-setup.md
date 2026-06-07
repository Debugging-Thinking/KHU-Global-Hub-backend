# 배포 자동화 · 도메인 · OTA 셋업 가이드

이 문서대로 **한 번만** 세팅하면, 이후엔:
- 백엔드 `main` push → EC2 자동 재배포
- 프론트 `main` push → 웹 자동 배포 + 앱 OTA 자동 갱신(재설치 X)
- IP 대신 `xxx.duckdns.org` 주소 + HTTPS

순서: **① GitHub Secrets → ② EC2 준비 → ③ DuckDNS+HTTPS → ④ 첫 빌드(아이콘/OTA 반영)**

---

## ① GitHub Secrets 등록

두 레포 각각 **Settings → Secrets and variables → Actions → New repository secret**.
(`gh` CLI로도 가능 — 아래 명령)

### 백엔드 레포 (`KHU-Global-Hub-backend`)
| Secret | 값 |
|--------|-----|
| `EC2_HOST` | EC2 공인 IP 또는 도메인 (예: `13.125.205.177`) |
| `EC2_USER` | `ubuntu` |
| `EC2_SSH_KEY` | `.pem` 개인키 **전체 내용**(`-----BEGIN ...` 포함) |

### 프론트 레포 (`KHU-Global-Hub-frontend`)
| Secret | 값 |
|--------|-----|
| `EC2_HOST` / `EC2_USER` / `EC2_SSH_KEY` | (백엔드와 동일) |
| `EXPO_PUBLIC_API_URL` | 운영 API 주소 **포트 없이** (예: `http://13.125.205.177`, HTTPS 후 `https://xxx.duckdns.org`) |
| `EXPO_TOKEN` | (OTA용) expo.dev → Account → Access Tokens 에서 발급. 없으면 OTA job만 실패하고 웹은 정상 |

```powershell
# gh CLI 예시 (.pem 키 등록)
gh secret set EC2_SSH_KEY --repo Debugging-Thinking/KHU-Global-Hub-backend < "C:\path\to\key.pem"
gh secret set EC2_HOST    --repo Debugging-Thinking/KHU-Global-Hub-backend --body "13.125.205.177"
gh secret set EC2_USER    --repo Debugging-Thinking/KHU-Global-Hub-backend --body "ubuntu"
# 프론트도 동일 + EXPO_PUBLIC_API_URL, EXPO_TOKEN
```

---

## ② EC2 준비 (한 번)

GitHub Actions가 ssh로 들어와 빌드 산출물을 올리고 재기동한다. 전제 조건:

1. **Java 21 설치** (백엔드 실행용)
   ```bash
   java -version   # 21 아니면: sudo apt install -y openjdk-21-jre-headless
   ```
2. **`~/.env` 존재** (운영 환경변수 — ARCHITECTURE §7 참고). 배포 스크립트가 `source ~/.env` 한다.
3. **passwordless sudo** (웹 배포가 `/var/www/html`에 sudo cp). Ubuntu 기본 계정은 보통 OK.
4. 웹 루트 디렉터리 존재: `sudo mkdir -p /var/www/html/globalhub`

> 배포 워크플로의 재기동 로직은 ARCHITECTURE §7의 운영 함정(self-excluding pkill,
> `ddl-auto=update` 강제)을 그대로 반영했다.

---

## ③ DuckDNS 무료 도메인 + HTTPS

### 3-1. DuckDNS 서브도메인 발급 (브라우저, 2분)
1. https://www.duckdns.org → GitHub/Google 로그인
2. 원하는 이름 입력 후 **add domain** (예: `khu-globalhub` → `khu-globalhub.duckdns.org`)
3. 그 도메인의 **current ip** 칸에 EC2 공인 IP 입력 후 저장
   - ⚠️ EC2 IP가 바뀌면 끊김 → **Elastic IP를 먼저 붙여 IP 고정** 권장 (`infra/terraform/ec2.tf`의 `aws_eip`)

### 3-2. Nginx에 도메인 등록 (EC2)
`/etc/nginx/sites-available/globalhub` 의 `server_name _;` →
```nginx
server_name khu-globalhub.duckdns.org;
```
```bash
sudo nginx -t && sudo systemctl reload nginx
```

### 3-3. Let's Encrypt HTTPS (EC2)
```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d khu-globalhub.duckdns.org
# 갱신은 자동(systemd timer). 테스트: sudo certbot renew --dry-run
```
→ 이제 `https://khu-globalhub.duckdns.org` 접속.

### 3-4. 프론트 API 주소 교체
- 프론트 레포 secret `EXPO_PUBLIC_API_URL` → `https://khu-globalhub.duckdns.org`
- HTTPS 적용 후엔 `app.json`의 `android.usesCleartextTraffic: true` 제거 가능(보안↑)

---

## ④ 첫 빌드 — 새 아이콘 + OTA 활성화

아이콘(네이티브 자산)과 `expo-updates`(네이티브 모듈)는 **OTA로 못 바꾼다 → 새 빌드 1회 필요.**
이 빌드부터 OTA가 작동한다.

```bash
cd frontend
eas login
eas build --profile preview --platform android   # APK 링크 → 폰 설치
```

이후 **JS만** 바뀐 기능은:
- `main`에 push → 워크플로가 `eas update --branch preview` 자동 실행 → 앱 껐다 켜면 반영 (재설치 X)
- 수동: `eas update --branch preview --message "설명"`

네이티브가 바뀔 때(새 expo 패키지/권한/SDK업/아이콘)만 `eas build` 재실행 + 재설치.

---

## 동작 요약

| 바꾼 것 | 필요한 동작 | 재설치 |
|---------|-------------|:---:|
| 백엔드 코드 | `main` push (자동 배포) | ❌ |
| 프론트 JS/화면 | `main` push (웹 배포 + OTA) | ❌ |
| 웹만 | `main` push | ❌ |
| 네이티브(아이콘/패키지/SDK) | `eas build` + 설치 | ✅ |
