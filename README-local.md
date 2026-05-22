# 로컬 개발 환경 셋업 가이드

KHU Global Hub 백엔드를 로컬에서 실행하는 방법입니다. 운영 서버와 별개로, 본인 PC에서 개발/테스트할 때 사용합니다.

---

## 1. 사전 준비 (최초 1회)

### 필수
| 항목 | 설명 |
|------|------|
| **JDK 21** | [Temurin 21](https://adoptium.net/temurin/releases/?version=21) 등 설치. `java -version` 으로 21 확인 |
| **Docker Desktop** | 로컬 PostgreSQL 실행용. [docker.com](https://www.docker.com/products/docker-desktop/) 에서 설치 |

> Docker를 쓰기 싫으면 로컬에 PostgreSQL을 직접 설치해도 됩니다.
> 그 경우 DB명 `khu_global_hub`, 유저 `globalhub`, 비번 `globalhub1234`, 포트 `5432` 로 맞추세요.

### 로컬 설정 파일 생성
`src/main/resources/application-local.yml` 은 git에 올라가지 않으므로 직접 만들어야 합니다.
같은 폴더의 `application-local.yml.example` 을 복사하세요.

```powershell
# 프로젝트 루트(backend)에서
Copy-Item src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

> 이 파일 안의 DB 접속 정보는 로컬 더미 값이라 민감하지 않습니다. 그대로 쓰면 됩니다.

---

## 2. 실행 (매번)

### 1) 로컬 DB 띄우기
Docker Desktop이 실행된 상태에서:
```powershell
docker compose up -d
```
(끌 때는 `docker compose down`. 데이터까지 지우려면 `docker compose down -v`)

### 2) 백엔드 실행
```powershell
.\gradlew bootRun
```

→ `http://localhost:8080` 에서 동작합니다.
→ API 문서(Swagger): `http://localhost:8080/swagger-ui.html`

---

## 3. 외부 서비스 키 (선택사항)

다음 기능들은 **실제 외부 키 없이도 앱은 정상 실행**되지만, 해당 기능을 실제로 테스트하려면 키가 필요합니다.

| 기능 | 필요한 키 |
|------|-----------|
| 회원가입 이메일 인증코드 발송 | `MAIL_USERNAME`, `MAIL_PASSWORD` |
| 글 작성 시 자동 번역 | `AZURE_TRANSLATOR_KEY`, `AZURE_REGION` |
| 이미지 업로드 (게시글/프로필) | `AWS_S3_BUCKET`, `AWS_S3_REGION`, `AWS_ACCESS_KEY`, `AWS_SECRET_KEY` |

키 값은 **팀 비밀채널/DM 등 안전한 경로로만** 공유받으세요. (git 커밋 금지 🚫)

키를 사용하려면 `gradlew` 실행 **전에** 같은 PowerShell 세션에서 환경변수로 설정:
```powershell
$env:MAIL_USERNAME="..."
$env:MAIL_PASSWORD="..."
$env:AZURE_TRANSLATOR_KEY="..."
$env:AZURE_REGION="koreacentral"
$env:AWS_S3_BUCKET="..."
$env:AWS_S3_REGION="ap-northeast-2"
$env:AWS_ACCESS_KEY="..."
$env:AWS_SECRET_KEY="..."
.\gradlew bootRun
```

키를 설정하지 않으면 위 기능 호출 시에만 실패하고, 나머지(게시판/Q&A/댓글/로그인 등)는 정상 동작합니다.

---

## 4. 자주 묻는 것

- **앱 재시작하면 데이터가 사라져요** → `application-local.yml` 의 `ddl-auto: create` 때문입니다. 데이터를 유지하려면 `update` 로 바꾸세요.
- **포트 5432가 이미 사용 중** → 로컬에 다른 PostgreSQL이 떠 있을 수 있습니다. 끄거나 `docker-compose.yml` 포트를 변경하세요.
- **`DB_URL` 관련 에러로 시작 실패** → `application-local.yml` 을 안 만들었을 가능성이 큽니다. (1번 마지막 단계 확인)
- **프론트 연동** → 프론트의 API base URL을 `http://localhost:8080` 으로 변경하면 로컬 백엔드와 통신합니다.
