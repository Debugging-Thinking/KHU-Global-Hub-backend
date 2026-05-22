# 로컬 개발 환경 셋업 가이드

KHU Global Hub 백엔드를 로컬에서 실행하는 방법입니다. 운영 서버와 별개로, 본인 PC에서 개발/테스트할 때 사용합니다.

---

## 1. 사전 준비 (최초 1회)

### 필수
| 항목 | 설명 |
|------|------|
| **JDK 21** | [Temurin 21](https://adoptium.net/temurin/releases/?version=21) 등 설치. `java -version` 으로 21 확인 |
| **Docker Desktop** | 로컬 PostgreSQL DB 실행용. [docker.com](https://www.docker.com/products/docker-desktop/) 에서 설치 |
| **Node.js** | 프론트(Expo) 실행용 (`npm install` / `npm run web`) |
| **Git** | 레포 clone |

> **왜 Docker?** 백엔드는 PostgreSQL DB가 필요합니다. 도커로 DB를 컨테이너로 띄우면
> PostgreSQL을 직접 설치·설정할 필요 없이 `docker compose up` 한 줄로 모든 팀원이 **동일한 DB 환경**을 얻습니다.
>
> Docker 없이 하려면 PostgreSQL을 직접 설치해도 됩니다 — DB명 `khu_global_hub`, 유저 `globalhub`,
> 비번 `globalhub1234` 로 맞추고 `application-local.yml` 포트를 본인 포트(보통 `5432`)로 변경하세요.

> **비밀번호 2종:**
> 
> |**DB 접속 비번** : `globalhub1234`  앱 ↔ PostgreSQL **연결**용. 설정 파일에 자동으로 들어감,  |
> 
> | **앱 로그인 비번** : `password123`  테스트 계정(demo/alice/bob/carol) **로그인**용. |
>
> docker/`dev.ps1`을 쓰면 `globalhub1234`는 볼 일도 없습니다. **네가 실제로 쓰는 건 `password123`(로그인) 뿐.**

### 로컬 설정 파일 (`application-local.yml`)
`src/main/resources/application-local.yml` 은 git에 올라가지 않습니다 — 각자 환경·비밀키를 넣는 개인 파일이라 유출 방지 목적으로 gitignore 처리.

> ✅ **아래 §2 "방법 A" `dev.ps1`/`dev.sh` 를 쓰면 이 파일을 `.example` 에서 자동 생성**하므로 **직접 만들 필요 없습니다.**
> 수동 실행(방법 B)이나 IDE에서 직접 띄울 때만 아래처럼 복사:

```powershell
Copy-Item src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

> DB 접속 정보는 로컬 더미 값(localhost:5433)이라 민감하지 않습니다. 그대로 쓰면 됩니다.
> (테스트 계정 demo/alice/bob/carol 은 이 설정과 별개로, 앱이 켜질 때 자동 시드됩니다 — §2 끝 참고)

---

## 2. 실행 (매번)

> 전제: `backend` 와 `frontend` 레포를 **같은 부모 폴더**에 클론해 두세요.
> (프론트도 최초 1회 `npm install` 필요)

### 방법 A) 원클릭 풀스택 실행 (권장) ⚡

DB + 백엔드 + 프론트를 한 번에 띄웁니다. Docker Desktop이 실행된 상태에서 `backend` 폴더에서:

```powershell
# Windows
powershell -ExecutionPolicy Bypass -File dev.ps1
```
```bash
# mac / linux
bash dev.sh
```

- DB(docker) 기동 → 백엔드/프론트를 각각 새 창(또는 백그라운드)에서 실행
- 프론트 `.env.local`(API 주소 = `http://localhost:8080`)을 자동 생성
- 프론트 폴더가 다른 위치면: `$env:GLOBALHUB_FRONTEND="C:\path\to\frontend"` 지정 후 실행

### 방법 B) 개별 실행 (수동)

```powershell
docker compose up -d          # 1) 로컬 DB (끌 때 docker compose down, 데이터까지: -v)
.\gradlew bootRun             # 2) 백엔드
# 3) 프론트: 별도 터미널에서 frontend 폴더로 가서  npm run web
```

→ 백엔드: `http://localhost:8080` · Swagger: `http://localhost:8080/swagger-ui.html`
→ 프론트(Expo Web): 실행 시 안내되는 브라우저 주소(보통 `http://localhost:8081`)

> 💡 **테스트 계정 자동 시드** — local 프로필은 **실행할 때마다** 테스트 계정·샘플 데이터를 초기화 후 재시드합니다 (회원가입/이메일 불필요, **항상 동일한 셋**).
> `demo` / `alice` / `bob` / `carol` `@khu.ac.kr` — 비밀번호 전부 `password123`. 두 명 동시 테스트는 **시크릿 탭**.
> (`LocalTestDataInitializer`, `@Profile("local")` + localhost DB만 — 운영 미적용. ⚠️ 매 실행 초기화라 로컬에서 직접 만든 데이터는 재시작 시 사라집니다.)

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

- **앱 재시작하면 내가 만든 데이터가 사라져요** → **의도된 동작**입니다. local 프로필은 매 실행마다 테스트 셋을 초기화 후 재시드(`LocalTestDataInitializer`)해 항상 동일한 상태로 시작합니다. 보존이 필요하면 그 초기화기의 `resetTestData()` 호출을 빼면 됩니다. (스키마는 Flyway 관리, `ddl-auto=update`)
- **DB 연결/인증 실패** → 도커 DB는 호스트 **5433** 포트로 매핑돼 있습니다 (로컬 네이티브 PostgreSQL의 5432와 충돌 방지용). `application-local.yml` 이 `localhost:5433` 을 가리키는지 확인하세요. 그래도 5433이 충돌하면 `docker-compose.yml` 과 `application-local.yml` 양쪽 포트를 비어있는 다른 번호로 바꾸면 됩니다.
- **`DB_URL` 관련 에러로 시작 실패** → `application-local.yml` 을 안 만들었을 가능성이 큽니다. (1번 마지막 단계 확인)
- **프론트 연동** → 프론트 기본 API 주소가 이미 `http://localhost:8080`입니다(`src/api/client.ts`). 로컬 백엔드를 띄웠으면 별도 설정 없이 붙습니다. (다른 서버를 보려면 `frontend/.env.local`에 `EXPO_PUBLIC_API_URL` 설정)
