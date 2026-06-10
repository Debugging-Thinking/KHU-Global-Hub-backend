# 🚀 신규 팀원 온보딩

처음 합류했다면 이 순서대로. 각 단계 끝나면 체크하세요.

## 1. 환경 준비
- [ ] Git · **JDK 21** · **Docker Desktop** · **Node.js(LTS)** 설치
- [ ] 백엔드·프론트 레포 둘 다 `git clone`

## 2. 로컬 실행 (상세: [localtest.md](./localtest.md))
- [ ] **backend**: `dev.ps1`(Windows) / `dev.sh` 원클릭 → Docker PostgreSQL(**5433**) + Spring Boot(**8080**) 기동
- [ ] **frontend**: `npm install` → `.env.local`에 `EXPO_PUBLIC_API_URL=http://localhost:8080` → `npx expo start --web`
- [ ] 테스트 계정 로그인: `demo@khu.ac.kr` / `password123` (관리자: `admin@khu.ac.kr` / `password123`)
- [ ] 로컬은 메일/Azure/AWS 키가 더미라 **키 없이도 실행됨**(해당 기능만 비활성)

## 3. 코드 구조 파악 (상세: [ARCHITECTURE.md](../ARCHITECTURE.md))
- [ ] 백엔드 = **Bounded Context(BC) 격리** — 내 BC만 알고 다른 BC는 `shared.port`/이벤트/ID 참조로만
- [ ] 내가 맡을 BC 확인 (ARCHITECTURE §2-3 소유권 표)
- [ ] 크로스-BC 규칙은 **ArchUnit이 강제** — 어기면 빌드 실패 (`BoundedContextRulesTest`)

## 4. 첫 PR (상세: [CONTRIBUTING.md](./CONTRIBUTING.md))
- [ ] `git checkout -b feature/내기능` (main에서 분기)
- [ ] 작업 후 `./gradlew test` 그린 확인
- [ ] `git push` → GitHub "Compare & pull request" → **PR 템플릿** 채우기
- [ ] 리뷰 승인 → **Squash merge** → 자동배포

## 막히면
- 로컬이 안 떠요 → [localtest.md](./localtest.md) 트러블슈팅
- PR/머지/배포 → [CONTRIBUTING.md](./CONTRIBUTING.md)
- **AI(Claude/Codex 등)에게 맡길 때** → `AGENTS.md`·`CONTRIBUTING.md`를 함께 읽히면 우리 규칙대로 작업해요
