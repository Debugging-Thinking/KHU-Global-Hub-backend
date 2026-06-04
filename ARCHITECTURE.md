# ARCHITECTURE.md — KHU Global Hub 백엔드 아키텍처

> 팀 개발 참고용 설계 문서. 새 기능 확장 전 이 문서를 먼저 읽어주세요.
> 민감 정보(실제 IP, 키, 엔드포인트)는 포함하지 않습니다 — 운영 접속 정보는 팀 내부 채널에서 관리.
> **코드 변경 시 이 문서도 함께 업데이트할 것.**
>
> 🛠 로컬 실행: [`localtest.md`](./localtest.md) · 팀 협업 규칙 요약: [`README.md` §10](./README.md) · 리팩토링 로그: [`docs/refactor-bc-isolation.md`](../Lim/refactor-bc-isolation.md)

---

## 1. 프로젝트 현황 (2026-05)

- **백엔드**: Spring Boot 3.4.5 / Java 21 / PostgreSQL 17 — AWS EC2 운영 배포 중
- **프론트엔드**: React Native (Expo SDK 52) + TypeScript — 핵심 기능 구현 완료, Expo Web으로 웹 배포
- **배포 URL**: `http://{EC2_IP}` (포트 80, Nginx static serving + `/api/` 프록시)
- **2026-05 BC 격리 리팩토링 완료**: 모놀리식 → Bounded Context 단위로 격리. 3인 팀이 각자 영역 소유.

---

## 2. 아키텍처 — Bounded Context(BC) 격리 ⭐

각 BC는 **자기 테이블/엔티티/규칙만 알고**, 다른 BC는 **ID·포트·이벤트로만** 안다.

### 2-1. 패키지 구조 (톱레벨 = BC)

```
com.khu.globalhub/
├── identity/      # 계정·인증·JWT·비밀번호 (Member)
├── profile/       # 프로필 (Profile, /api/members)
├── board/         # 게시판(자유 1종) + 댓글(흡수) + 좋아요/이미지
├── qna/           # 질문 + 답변(채택) + 좋아요
├── chat/          # 1:1 DM
├── mentoring/     # 멘토-멘티 매칭 + 스케줄러
├── campusguide/   # 퀴즈 (+ 학사 가이드 예정)
├── devsupport/    # 로컬 테스트 데이터 시드 (@Profile("local") 전용)
└── shared/        # 전역 공통 — 어떤 BC도 import 하지 않음
    ├── port/      # 크로스-BC 계약 인터페이스: ProfileQueryPort, MemberQueryPort, ProfileGateway
    ├── extevent/  # 통합 이벤트: QuizCompletedEvent, MatchCreatedEvent
    ├── anonymous/ # 익명 번호 (supporting)
    ├── config/ exception/ jwt/ infra/ common/ enums/ util/
```

### 2-2. BC 내부 4계층 (의존 방향 단방향)

```
presentation ─┐
              ├─► application ─► domain
infrastructure┘
```
- **domain**: 엔티티 + 도메인 로직. JPA 애노테이션 OK, `@Service` 등 스프링 스테레오타입 금지. 같은 BC domain만 참조.
- **application**: 유스케이스(서비스), 이벤트 리스너. domain + 자기 BC repository 주입.
- **infrastructure**: repository, 외부 어댑터(포트 구현). domain/application 알아도 됨.
- **presentation**: controller + 요청/응답 DTO.

### 2-3. BC 소유권 (분담)

| BC | 소유자 | 핵심 엔티티 |
|----|--------|------------|
| identity | 본인 | Member |
| profile | 본인 | Profile |
| board | 본인 | Post, PostLike, PostImage, Comment, CommentLike, *Translation |
| qna | 본인 | QnA, Answer, QnALike, AnswerLike, *Translation |
| chat | 현우 | ChatMessage |
| mentoring | 현우 | MentorMenteeMatch |
| campusguide | 태경 | QuizQuestion, QuizResult |

### 2-4. 크로스-BC 규칙 (ArchUnit으로 강제)

- ❌ BC가 다른 BC 패키지를 import 금지 (`shared` 제외)
- ❌ `shared`가 BC 패키지를 import 금지
- ❌ `domain` 계층이 상위 계층(application/infra/presentation) import 금지
- ✅ 통신은 **① ID 참조**(엔티티 직접참조 금지, `@ManyToOne Member` → `Long memberId`) **② `shared.port` 포트** **③ `shared.extevent` 통합 이벤트** 로만
- 규칙은 `src/test/.../architecture/BoundedContextRulesTest.java`(ArchUnit 8규칙)가 검증 → 어기면 빌드 실패
- **DB 외래키(FK)는 유지** — 코드에서 `@ManyToOne`을 빼는 것과 DB FK 삭제는 별개. 단일 DB 모놀리식에서는 FK가 참조 무결성 보장.

### 2-5. 현재 노출된 포트 / 이벤트

`shared.port` (인터페이스 — 구현은 소유 BC가 `@Component`로 제공):
```java
ProfileQueryPort   // 구현: profile.application.ProfileQueryAdapter
  Optional<String> findName(Long memberId);
  Optional<ProfileCard> findCard(Long memberId);          // ProfileCard(name, profileImage)
MemberQueryPort    // 구현: identity.application.MemberQueryAdapter
  boolean exists(Long memberId);
  Optional<String> findEmail(Long memberId);
ProfileGateway     // 구현: profile.application.ProfileGatewayAdapter (identity가 프로필 생성/존재확인에 사용)
  boolean exists(Long memberId);
  void create(ProfileCreationCommand command);
```
`shared.extevent` (통합 이벤트 — `@TransactionalEventListener(AFTER_COMMIT)`로 소비):
```java
QuizCompletedEvent(Long memberId, double score)    // campusguide 발행 → profile이 quizScore 갱신
MatchCreatedEvent(Long mentorId, Long menteeId)     // mentoring 발행 → chat이 시스템 메시지 삽입
```
> 다른 BC 상태를 바꿔야 하면 **이벤트 발행**(payload는 ID/단순값만), 단순 조회는 **포트** 사용. 남의 Repository 직접 주입 금지.

---

## 3. 도메인 설계 핵심 결정사항

### 3-1. Member / Profile 분리 (identity ↔ profile)
- `members`(identity): 인증 전용 (email, password, refreshToken, isEmailVerified)
- `profiles`(profile): 프로필 데이터 (name, department, nationality, admissionYear, language, mentoringRole, quizScore, bio). `member_id`로 identity 참조(ID only).
- Profile row 존재 여부 = 프로필 완성. 로그인 응답 `hasProfile` 플래그로 프론트가 프로필 생성 화면 라우팅.
- 프로필 생성(`POST /api/auth/profile`)은 identity가 `ProfileGateway`로 profile에 위임.

### 3-2. JWT 구조
- AccessToken 24시간(stateless, DB 미저장) / RefreshToken 7일(`members.refresh_token` 저장)
- SecurityContext principal = `Long memberId` · `SecurityUtil.getCurrentMemberId()`로 조회

### 3-3. 번역 비동기 처리
- 작성 시 원문 Translation 행 **동기** 저장 → 나머지 5개 언어 `@Async("translationExecutor")` 번역
- 번역 실패/미존재 → 조회 시 EN 폴백 → 그것도 없으면 첫 번째 번역본. 스레드풀 core=4/max=8.

### 3-4. S3 이미지 업로드
- 게시글 이미지: 저장 후 `@Async("s3Executor")` 업로드. MultipartFile을 요청 스코프 종료 전 `byte[]`로 읽어 `ImageData` record 전달.
- URL 패턴: 게시글 `posts/{postId}/{uuid}` · 프로필(동기) `profiles/{memberId}/{uuid}`

### 3-5. 익명 번호 시스템 (shared.anonymous)
- DB엔 항상 `author_id` 저장(삭제/신고용). `isAnonymous=true`면 `authorName`="익명N".
- `AnonymousAlias (contextType, contextId, memberId)` 유니크 조합으로 번호 할당.
  - `AliasContextType.POST` — 게시글 1개(+댓글) = 1 컨텍스트
  - `AliasContextType.QNA` — Q&A 1개(+답변) = 1 컨텍스트 공유
- 작성자=익명1, 이후 순번. 같은 사람·같은 컨텍스트면 동일 번호 유지. 게시글/Q&A 컨텍스트는 서로 독립.

### 3-6. 게시판 (board) — 1종 (D2)
- 게시판 종류 구분 폐지(`boardType`/`BoardType` 제거). 자유게시판 1종.
- 프론트는 board + qna를 한 페이지 [자유게시판 | QnA] 2탭으로 표시.

### 3-7. 댓글 — board 전용 (D3/D4)
- Comment는 **게시글 전용** (board BC 소유). 구 generic 모델(POST/QNA/ANSWER) 폐기, `CommentTargetType` 제거.
- `comments.target_id` = 게시글 ID. 대댓글은 `parent_id` 자기참조.
- 삭제·좋아요 공통 엔드포인트: `DELETE /api/comments/{id}`, `POST /api/comments/{id}/like`
- `Post.commentCount` 비정규화 컬럼 — 댓글/대댓글 작성 시 증가, 삭제 시 감소.

### 3-8. Q&A 비즈니스 규칙 (qna)
- 채택 완료 질문엔 답변 불가(`QNA_ALREADY_ADOPTED`) · 본인 질문 답변 불가(`SELF_ANSWER_NOT_ALLOWED`) · 1인 1답(`ANSWER_ALREADY_EXISTS`)
- 채택 시 `Answer.isAdopted = QnA.isAdopted = true` (1회만). **Q&A 댓글 기능 없음**(D3).

### 3-9. 채팅 (chat) — DM
- `ChatRoom` 테이블 없음. `sender_id + receiver_id` 조합으로 대화 식별. (전부 `Long` ID)
- 시스템 메시지: `senderId=null, isSystem=true, contextPartnerId`로 대화 귀속. 멘토링 매칭 시 `MatchCreatedEvent` 수신해 삽입.
- 읽음 처리: 대화 조회 시 자동 일괄. 현재 폴링 방식.

### 3-10. 멘토링 (mentoring) — 스케줄러
- `@Scheduled` 매년 3/1, 9/1 자정(UTC). 매칭 알고리즘: **점수제 그리디** — 같은 국적 +3 / 같은 언어 +2 / 멘토가 1~2년 선배 +1 → 점수 내림차순 1:1 매칭 후 남는 인원은 라운드로빈 배정. (학과는 점수에서 제외) 3월엔 매칭 전 전년도 입학 멘티를 MENTOR로 자동 승격.
- 매칭 생성 시 `MatchCreatedEvent` 발행(chat이 시스템 메시지 삽입). 매칭은 `mentor_id`/`mentee_id`(Long ID).
- 수동 매칭 트리거 `POST /api/mentoring/run`은 **로컬 전용**(`@Profile("local")` · `MentoringDevController`) — 운영 미노출. 운영 매칭은 스케줄러만 수행.
- **멘토링 활동 기록**: 매칭 당사자(멘토/멘티)만 작성·조회. `mentoring_activities`(match_id·author_id·title·content, V6).

### 3-11. 퀴즈 (campusguide)
- 응시 채점 후 `QuizResult` 저장 + `QuizCompletedEvent(memberId, score)` 발행 → profile이 최고점수(quizScore) 갱신. (campusguide는 profile을 모름)

### 3-12. 비밀번호 재설정 (identity)
- `forgot-password`: 이메일 인증된 계정만 코드 발송(10분). `reset-password`: 코드 검증 후 적용, 성공 시 refreshToken 무효화(전 기기 로그아웃).

---

## 4. 데이터 / 스키마 (Flyway)

- **Flyway로 스키마 관리** (ddl-auto 졸업). 운영은 `validate`.
- 마이그레이션 (`src/main/resources/db/migration/`):
  | 버전 | 내용 |
  |------|------|
  | `V1__baseline.sql` | 현재 스키마 스냅샷(21테이블). 운영엔 baseline-on-migrate로 기록만 |
  | `V2__delete_qna_answer_comments.sql` | qna/답변 댓글 데이터 삭제 (D3, 비가역) |
  | `V3__drop_comment_target_type.sql` | `comments.target_type` 컬럼 제거 |
  | `V4__drop_post_board_type.sql` | `posts.board_type` 컬럼 제거 |
- 스키마 변경은 **반드시 새 Flyway 마이그레이션**으로. 운영 적용 전 **RDS 스냅샷** 필수.
- `@ManyToOne Member` → `Long memberId`는 매핑 컬럼(`*_id`) 동일 → 스키마 무변경(기존 데이터 그대로). DB FK 제약 유지.

---

## 5. API 전체 목록

### Auth — `/api/auth`
| Method | Path | 설명 |
|--------|------|------|
| POST | /register | 이메일 인증 코드 발송 (@khu.ac.kr 전용) |
| POST | /verify-email | 코드 확인 + JWT 발급 (hasProfile 포함) |
| POST | /profile | 최초 프로필 생성 (신입생=MENTEE 강제) |
| POST | /login | 로그인 |
| POST | /refresh | 액세스 토큰 갱신 |
| POST | /logout | refresh token null 처리 |
| POST | /forgot-password | 비밀번호 재설정 코드 발송 (10분) |
| POST | /reset-password | 코드 검증 후 새 비번 적용 (전 기기 로그아웃) |

### Member(profile) — `/api/members`
| Method | Path | 설명 |
|--------|------|------|
| GET | /me · PUT /me | 내 프로필 조회/수정 |
| PATCH | /me/mentoring-role · /me/profile-image | 역할 변경 / 이미지 업로드(multipart) |
| GET | /{memberId} | 타인 프로필 |
| GET | /{memberId}/posts | 특정 멤버 게시글 목록 (board BC가 제공) |

### Board — `/api/posts`
| Method | Path | 설명 |
|--------|------|------|
| POST | / | 게시글 작성 (multipart, 이미지 가능 — **boardType 없음**) |
| GET | /?language=KO | 게시글 목록 (페이징, 게시판 1종) |
| GET | /popular?language=KO | 인기 게시물 (좋아요 10+) |
| GET | /{postId}?language=KO | 게시글 상세 |
| DELETE | /{postId} · POST /{postId}/like | 삭제(작성자만, cascade) / 좋아요 토글 |

### Comment (board 소유, 게시글 전용)
| Method | Path | 설명 |
|--------|------|------|
| POST/GET | /api/posts/{postId}/comments | 게시글 댓글 작성/목록 (parentId=대댓글) |
| DELETE | /api/comments/{commentId} | 댓글 삭제 (대댓글+좋아요 cascade) |
| POST | /api/comments/{commentId}/like | 댓글 좋아요 토글 |
> ⚠️ QnA·답변 댓글 API는 폐기됨 (D3).

### Q&A — `/api/qnas`
| Method | Path | 설명 |
|--------|------|------|
| POST/GET | / , /{qnaId} | 질문 작성/목록/상세(+답변) |
| DELETE | /{qnaId} · POST /{qnaId}/like | 질문 삭제(cascade) / 좋아요 |
| POST | /{qnaId}/answers | 답변 작성 (채택후 불가, 본인질문 불가, 1인1답) |
| DELETE | /{qnaId}/answers/{answerId} | 답변 삭제 |
| POST | /{qnaId}/answers/{answerId}/adopt · /like | 채택(질문자만,1회) / 좋아요 |

### Mentoring — `/api/mentoring`
| Method | Path | 설명 |
|--------|------|------|
| GET | /me | 내 현재 ACTIVE 매칭 (상대 프로필 포함) |
| GET | /me/history | 내 전체 매칭 이력 |
| GET·POST | /{matchId}/activities | 멘토링 활동 기록 조회/작성 (매칭 당사자만) |
| POST | /run | 수동 매칭 트리거 — **로컬 전용**(`@Profile("local")`) |

### Chat — `/api/chat`
| POST / · GET / · GET /{partnerId} | 메시지 전송 / DM 목록 / 대화 내용+읽음 처리 |

### Quiz(campusguide) — `/api/quiz`
| GET /questions · POST /submit · GET /results/me · GET /score/me | 문항/제출/내 기록/최고점수 |

---

## 6. 공통 규칙 (프론트 연동 필수)

- **인증 헤더**: `Authorization: Bearer {accessToken}`
- **공통 응답**: `{ "success": true, "message": "ok", "data": {...} }` (실패 시 success=false, data=null, message=에러)
- **언어 파라미터**: `?language=KO` (KO/EN/ZH/VI/ES/MN), 생략 시 KO
- **페이지네이션**: `?page=0&size=20`
- **인증 플로우**: register → verify-email(accessToken+refreshToken+hasProfile) → (hasProfile=false면) profile 생성 → 이후 Bearer 토큰 → 401 시 refresh

---

## 7. 운영 배포 정보

### 인프라
- EC2 (ap-northeast-2, t3.micro) — 백엔드(8080) + Nginx 웹(80)
- RDS PostgreSQL 17 · S3 (이미지) · Azure Translator Free F0

> **실제 IP, SSH 키, RDS 엔드포인트는 팀 내부 채널에서 관리.**

### 백엔드 재배포
```bash
./gradlew bootJar
scp -i "{KEY}.pem" build/libs/globalhub-0.0.1-SNAPSHOT.jar ubuntu@{EC2_IP}:~/app.jar
ssh -i "{KEY}.pem" ubuntu@{EC2_IP} "nohup bash ~/deploy.sh > /tmp/deploy.log 2>&1 &"
# deploy.sh: pkill -f app.jar; sleep 2; source ~/.env; nohup java -jar ~/app.jar > ~/app.log 2>&1 &
```
> ⚠️ 이번 배포(D2/D3)에는 비가역 마이그레이션(V2~V4: qna댓글 삭제·컬럼 제거)이 포함 — **RDS 스냅샷 먼저, 프론트와 동시 배포**.

### 프론트 웹 재배포
```bash
cd frontend && npx expo export --platform web      # dist/ 생성
scp -i "{KEY}.pem" -r dist/* ubuntu@{EC2_IP}:~/web/
ssh -i "{KEY}.pem" ubuntu@{EC2_IP} "sudo cp -r ~/web/* /var/www/html/globalhub/"
```

### Nginx (EC2 /etc/nginx/sites-available/globalhub)
```nginx
server {
    listen 80; server_name _;
    root /var/www/html/globalhub; index index.html;
    location / { try_files $uri $uri/ /index.html; }
    location /api/ {
        proxy_pass http://localhost:8080;   # 끝슬래시 X — /api/ prefix 유지
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### EC2 환경변수 (~/.env)
```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://{RDS_ENDPOINT}:5432/khu_global_hub
DB_USERNAME=... DB_PASSWORD=... DDL_AUTO=validate
JWT_SECRET=... AZURE_TRANSLATOR_KEY=... AZURE_REGION=...
AWS_S3_BUCKET=... AWS_S3_REGION=... AWS_ACCESS_KEY=... AWS_SECRET_KEY=...
MAIL_USERNAME=... MAIL_PASSWORD=...
```
> Flyway가 스키마를 관리하므로 운영은 `validate` 유지. 스키마 변경은 새 마이그레이션으로만.

---

## 8. 프론트엔드 구조

- Expo SDK 52 + React Native + TypeScript · expo-router · Zustand(`authStore`) · axios(401 자동 갱신)
- API 주소: `src/api/client.ts` — `EXPO_PUBLIC_API_URL` 미설정 시 운영 IP 폴백. 로컬은 `.env.local`에 `http://localhost:8080`.
```
frontend/app/(main)/
├── index.tsx              # 메인 — [자유게시판 | QnA] 2탭 (board+qna 통합)
├── board/[postId], create # 게시글 상세·작성 (board/index → 메인 리다이렉트)
├── qna/[qnaId], create    # 질문 상세·작성
├── chat/, mentoring, profile, quiz
```
- 플랫폼 분기: `Alert.alert`은 웹 미동작 → `confirmAction` 헬퍼(웹은 `window.confirm`).

---

## 9. 테스트 / 검증 (안전망)

- **Testcontainers**(실 PostgreSQL 17) + **Flyway**(V1~)로 운영과 동일 스키마 위에서 검증.
- **characterization 테스트**: 인증/게시판/댓글/Q&A/채택/좋아요/채팅/멘토링/퀴즈 핵심 경로 동작 박제.
- **ArchUnit**(`BoundedContextRulesTest`): BC 의존 경계 강제.
- 검증: `./gradlew test`. 리팩토링·기능 추가 후 그린이면 "동작 불변 + 경계 유지" 보장.
- 로컬은 `LocalTestDataInitializer`(@Profile local)가 테스트 계정·데이터 자동 시드 → localtest.md 참고.

---

## 10. 보류 항목

| 항목 | 비고 |
|------|------|
| 학사 가이드 백엔드 | campusguide BC에 추가 예정 (퀴즈와 같은 BC) |
| Q&A 채택 마일리지 | 채택 구현 완료, 마일리지 추후 |
| 게시글 신고/블라인드 · 푸시 알림 | Phase 2 |
| 게시글·댓글 수정 API | 삭제만 구현 |
| 이메일 인증 코드 재발송 | 미구현 |
| 강의평(coursereview)·시간표(lecturecatalog) | 신규 BC, 추후 |

---

## 11. 알려진 기술 부채

| 항목 | 설명 |
|------|------|
| mentoring → profile 결합 | 매칭 알고리즘이 Profile(역할/입학년도) 직접 조회 → 추후 포트화 |
| shared.infra → BC 엔티티 | `TranslationService`/`S3Service`가 board/qna 엔티티에 결합(영속화) → 추후 분리 |
| Scheduler 시간대 | cron이 서버 JVM(UTC) 기준 → KST와 9시간 차 |
| S3 고아 파일 | 게시글/프로필 이미지 교체·삭제 시 기존 S3 파일 미삭제 |
| commentCount 동시성 | 수동 증감 — 대규모 동시요청 시 race 가능 |
| 채팅 폴링 | 실시간성 낮음. 사용자 증가 시 WebSocket 전환 권장 |
| APK 빌드 | 최신 프론트로 EAS Build 재실행 필요 |

---

## 12. 개발 환경 주의사항

- 로컬 실행: [`localtest.md`](./localtest.md) (`dev.ps1`/`dev.sh` 원클릭 — 카톡 공유, gitignore)
- `application-local.yml`·`application-prod.yml`은 `.gitignore` (dev 런처가 `.example`에서 자동 생성). `*.pem`, `*.env`, `CLAUDE.md`도 gitignore.
- 메일/Azure/AWS 키는 더미 기본값이 있어 **키 없이도 앱 실행 가능**(해당 기능만 비활성). 실제 테스트 시 환경변수 주입.
- 로컬 DB는 docker 포트 **5433**(네이티브 PostgreSQL 5432 충돌 방지). Flyway가 스키마 생성.
- 신규 기능: 자기 BC 패키지 안 4계층으로. 다른 BC 필요 시 `shared.port`/이벤트. 스키마 변경은 Flyway. PR 전 `./gradlew test` 그린 확인.
