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
├── translation/   # on-demand 텍스트 번역 (POST /api/translate) — 6개 외 언어/채팅용
├── media/         # 이미지 업로드 (POST /api/images → S3) — 댓글/Q&A/답변/채팅 공용
├── coursereview/  # 강의평 (강의 목록 + 익명 리뷰 + 수업지표 집계)
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
| translation | 본인 | (무상태 — `shared.infra.AzureTranslateClient` 위임) |
| media | 본인 | (무상태 — `shared.infra.S3Service.uploadImage` 위임) |
| coursereview | 본인 | Lecture, CourseReview (+ AttendanceType/FrequencyLevel 지표) |

### 2-4. 크로스-BC 규칙 (ArchUnit으로 강제)

- ❌ BC가 다른 BC 패키지를 import 금지 (`shared` 제외)
- ❌ `shared`가 BC 패키지를 import 금지
- ❌ `domain` 계층이 상위 계층(application/infra/presentation) import 금지
- ✅ 통신은 **① ID 참조**(엔티티 직접참조 금지, `@ManyToOne Member` → `Long memberId`) **② `shared.port` 포트** **③ `shared.extevent` 통합 이벤트** 로만
- 규칙은 `src/test/.../architecture/BoundedContextRulesTest.java`(ArchUnit 9규칙)가 검증 → 어기면 빌드 실패
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
- `profiles`(profile): 프로필 데이터 (name, department, nationality, admissionYear, language, **preferred_language**, mentoringRole, quizScore, bio). `member_id`로 identity 참조(ID only).
- **선호 언어 모델(V9)**: `preferred_language`(Azure 코드, 예 `fr`·`ja`·`ko`)가 실제 선택값. `language`(6개 enum)는 여기서 파생한 **버킷** — 6개 코드(ko/en/zh-Hans/vi/uz/mn-Cyrl)와 정확히 일치하면 그 언어, 그 외는 **EN**. 정적 UI/사전번역 콘텐츠 선택엔 `language`, on-demand 번역 목표 언어엔 `preferred_language`를 쓴다. (`department`·`nationality`는 프론트가 코드로 저장하나 DB는 String 그대로 — 마이그레이션 불필요, 레거시 자유텍스트는 표시 폴백)
- Profile row 존재 여부 = 프로필 완성. 로그인 응답 `hasProfile` 플래그로 프론트가 프로필 생성 화면 라우팅.
- 프로필 생성(`POST /api/auth/profile`)은 identity가 `ProfileGateway`로 profile에 위임.

### 3-2. JWT 구조
- AccessToken 24시간(stateless, DB 미저장) / RefreshToken 7일(`members.refresh_token` 저장)
- SecurityContext principal = `Long memberId` · `SecurityUtil.getCurrentMemberId()`로 조회

### 3-3. 번역 — 사전번역(6개) + on-demand(그 외/채팅)
- **공통 클라이언트**: `shared.infra.AzureTranslateClient`(엔티티 비결합 — texts·toCodes·from? → 번역+detectedLanguage)를 ① 사전번역 ② on-demand 둘 다 재사용.
- **① 사전번역(6개 언어 사용자)**: 작성 시 원문 Translation 행 **동기** 저장 → `@Async("translationExecutor")`(core=4/max=8)로 나머지 6개 언어 저장. 조회는 `?language=`(요청언어→EN→첫행 폴백). 게시글/Q&A 상세엔 원문/번역 토글.
- **원문 언어 자동 감지**: Azure translate 호출 시 `from` 미지정 → 원문 언어 자동 감지(예: 한국인이 영어로 작성 → EN 인식). 감지는 translate 응답(`detectedLanguage`)에 포함 → 별도 detect 호출 없음. 감지 언어로 원문 행 라벨 보정 후 나머지 번역 저장. (`Language.fromAzureCode` 역매핑, 미지원이면 claimed 폴백)
- **② on-demand(6개 외 언어 사용자 + 채팅)**: 정적 UI=EN(버킷), 콘텐츠는 **원문**으로 제공(읽기 API `?original=true` → 소스 행). 사용자가 "번역하기"를 누르면 `POST /api/translate`(target=preferred_language)로 즉시 번역(프론트 캐시·원문 토글). 채팅(1:1)은 항상 동적 — 받은 메시지 "번역" 탭 시에만. 사전 6버전을 저장하지 않아 빠르다.
- 번역 실패/미존재 → 조회 시 EN 폴백 → 그것도 없으면 첫 번째 번역본.

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
- **다국어 DB(V16)**: `QuizQuestion`은 메타(category·answerIndex)만 보유, 텍스트(question·options·explanation)는 `QuizQuestionTranslation`(language enum, options는 JSON 직렬화 문자열)으로 분리 — 게시판식 사전번역. 관리자 생성/수정 시 KO 원문 동기 저장 + `QuizTranslationWriter @Async`로 나머지 5개 언어 번역(질문 1+보기 N+해설 1을 한 번에 보내 인덱스로 분해). 조회는 `?language=`(요청언어→KO→첫행 폴백). 채점은 answerIndex 기준이라 언어 무관.

### 3-12. 비밀번호 재설정 (identity)
- `forgot-password`: 이메일 인증된 계정만 코드 발송(10분). `reset-password`: 코드 검증 후 적용, 성공 시 refreshToken 무효화(전 기기 로그아웃).

### 3-13. 강의평 (coursereview)
- `Lecture`(강의 카탈로그: code/name/professor/college/type/credits/semester) + `CourseReview`(별점 1~5 + 본문). **리뷰는 익명 노출**(작성자 이름 미표시), 단 `author_id`는 저장(본인 삭제·중복/신고용 — 응답 `isMine`으로 삭제버튼 제어).
- **수업지표(에타식, 리뷰별 응답 → 강의 단위 집계)**: 수업방식 `AttendanceType`(대면/비대면/혼합) + 발표·조모임·과제·한국어사용 `FrequencyLevel`(적음/보통/많음). **모두 선택 입력**(null 허용). 상세 조회 시 `IndicatorSummary`로 옵션별 카운트 집계(0 버킷 포함, null 제외 → 합이 reviewCount보다 작을 수 있음). 한국어 사용 "적음" = 유학생 친화 신호.
- 강의 검색: `name/professor/code` LIKE(학기 필터, 기본 `2026-1`). 다른 BC 의존 없음(완전 격리). 프론트는 입력 즉시(디바운스 300ms) 검색.
- **번역**: 강의평 본문도 게시판과 동일한 사전번역(6개) + on-demand 토글 — `course_review_translations`(V13), `CourseReviewTranslationWriter @Async`. 읽기 `?language=`/`&original=true` 지원.
- **실제 강의 데이터**: 경희대 국제캠 2026-1 **2141건** 임포트(`KhuSugangClient`가 sugang.khu.ac.kr `lectListJson` loginYn=N 공개 엔드포인트 스크랩 → `GLOBAL_COLLEGE_CODES` 필터). `POST /api/lectures/import`로 적재(로컬·운영 모두 완료).

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
  | `V5`~`V7` | 멘토링 활동기록·뱃지 등 (각 기능 PR) |
  | `V8__swap_es_to_uz_language.sql` | 지원 언어 ES(스페인어) → UZ(우즈벡어) 교체 + CHECK 제약 갱신 |
  | `V9__add_profile_preferred_language.sql` | `profiles.preferred_language`(Azure 코드, nullable) 추가 + 기존행 backfill(language→코드) |
  | `V10__add_image_url.sql` | 댓글/Q&A/답변/채팅에 `image_url` 추가 (media BC 이미지 업로드) |
  | `V11__create_course_review.sql` | `lectures` + `course_reviews` 테이블 생성 (강의평) |
  | `V12__add_course_review_indicators.sql` | `course_reviews`에 수업지표 5종 컬럼 추가 (attendance_type/presentation_freq/group_work_freq/assignment_freq/korean_usage, 모두 nullable) |
  | `V13__create_course_review_translation.sql` | `course_review_translations` 생성 (강의평 6개 언어 사전번역 — 게시판식 번역 시스템) |
  | `V16__quiz_multilang.sql` | `quiz_question_translations` 생성 + 기존 문항(question/explanation)·`quiz_options`를 KO 번역행으로 이관 후 `quiz_questions.question/explanation`·`quiz_options` 제거 (퀴즈 다국어 전환) |
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
| PUT | /{postId} | 게시글 수정 (multipart, 작성자만 — 본문/언어 upsert + 재번역, 이미지 추가) |
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
| Method | Path | 설명 |
|--------|------|------|
| POST / · GET / · GET /{partnerId} | 메시지 전송(텍스트/이미지/파일) / DM 목록 / 대화 내용+읽음 처리 |
| DELETE | /{messageId} | 메시지 삭제 — **본인이 보낸 메시지만**(프론트는 길게누르기) |
> DM 목록의 마지막 메시지가 이미지/파일 전용(content 없음)이면 `📎`로 미리보기. 채팅방은 상대 프로필(헤더 이름·아바타 + 받은 메시지 아바타)을 `GET /members/{partnerId}`로 표시.

### Quiz(campusguide) — `/api/quiz`
| GET /questions?language=KO · POST /submit · GET /results/me · GET /score/me | 문항(요청언어 번역)/제출/내 기록/최고점수 |

### Admin Quiz(campusguide) — `/api/admin/quiz/questions` (AdminGuard)
| Method | Path | 설명 |
|--------|------|------|
| POST | / | 퀴즈 문항 생성 (category·answerIndex·KO question/options/explanation → 원문 저장 + 6개 언어 비동기 번역) |
| PUT | /{questionId} | 퀴즈 문항 수정 (메타 갱신 + 원문 upsert + 재번역) |
| DELETE | /{questionId} | 퀴즈 문항 삭제 (번역행 cascade) |

### Translation — `/api/translate`
| Method | Path | 설명 |
|--------|------|------|
| POST | / | on-demand 번역 `{texts[], target, source?}` → `{translations[], detectedSource}` (콘텐츠 "번역하기"·채팅 "번역" 공용) |
> Board/Q&A/Comment 읽기 엔드포인트는 `&original=true` 지원 — 번역본 대신 **원문(소스) 행** 반환(6개 외 언어 사용자용).

### Media — `/api/images`
| Method | Path | 설명 |
|--------|------|------|
| POST | / | 파일 업로드(multipart `image`, **이미지 외 일반 파일도 허용**) → S3 → `{url}` (댓글/Q&A/답변/채팅이 `imageUrl`로 참조, 프론트 `Attachment`가 이미지=확대·파일=클립칩 렌더) |

### Course Review(coursereview) — `/api/lectures`
| Method | Path | 설명 |
|--------|------|------|
| GET | /?semester=2026-1&query= | 강의 검색 목록 (이름/교수/코드 LIKE, 페이징) |
| GET | /{lectureId} | 강의 상세 + 지표 집계(`indicators`) + 강의평 목록(익명) |
| POST | /{lectureId}/reviews | 강의평 작성 (별점·본문 필수, 지표 5종 선택) |
| DELETE | /reviews/{reviewId} | 강의평 삭제 (작성자 본인만) |
| POST | /import | 경희대 수강신청 시스템에서 강의 카탈로그 적재 (KhuSugangClient 스크랩) |

---

## 6. 공통 규칙 (프론트 연동 필수)

- **인증 헤더**: `Authorization: Bearer {accessToken}`
- **공통 응답**: `{ "success": true, "message": "ok", "data": {...} }` (실패 시 success=false, data=null, message=에러)
- **언어 파라미터**: `?language=KO` (KO/EN/ZH/VI/UZ/MN 버킷 — 한/영/중/베트남/우즈벡/몽골), 생략 시 KO. 6개 외 언어 사용자는 `&original=true`로 원문을 받고 `POST /api/translate`로 번역.
- **선호 언어**: 프로필은 Azure 코드(`preferredLanguage`, 182개 중 택1)로 저장. 백엔드가 6개 버킷(`language`)을 파생. 프론트 정적 UI는 버킷(6개면 그 언어, 그 외 EN) 기준.
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
./gradlew bootJar -x test
scp -i "{KEY}.pem" build/libs/globalhub-0.0.1-SNAPSHOT.jar ubuntu@{EC2_IP}:~/app.jar
# 재기동: ~/.env export 후 setsid java -jar --ddl-auto=update (CLI로 최우선 강제)
```
> ⚠️ **운영 기동 함정**: ① 운영 스키마가 baseline 드리프트로 `validate` 실패 → CLI `--spring.jpa.hibernate.ddl-auto=update`로 강제(컬럼 추가만, 삭제 X = 데이터 안전). ② `pkill -f app.jar`는 ssh 셸 cmdline에도 'app.jar'가 있어 자기 자신을 죽임 → self-excluding 패턴(`pkill -f 'active=pro[d]'`) 사용. ③ 비가역 마이그레이션 포함 배포 전 **RDS 스냅샷** 필수.

### 프론트 웹 재배포
```bash
cd frontend
# ⚠️ 반드시 운영 IP 주입 + 캐시 클리어! (아래 함정 참고)
EXPO_PUBLIC_API_URL=http://{EC2_IP} npx expo export --platform web --clear   # dist/ 생성
scp -i "{KEY}.pem" -r dist/* ubuntu@{EC2_IP}:~/web/
ssh -i "{KEY}.pem" ubuntu@{EC2_IP} "sudo rm -rf /var/www/html/globalhub/* && sudo cp -r ~/web/* /var/www/html/globalhub/"
```
> 🚨 **함정(로그인 전체 장애 유발함)**: `src/api/client.ts`의 `BASE_URL`은 `EXPO_PUBLIC_API_URL` 미주입 시 **localhost:8080** 폴백. 미주입 배포본은 개발자 PC 로컬 백엔드에 붙어 "되는 척"하다가 로컬을 내리면 전원 로그인 실패. 빌드 시 **운영 IP를 포트 없이**(`http://{EC2_IP}` — 80포트 Nginx가 `/api/` 프록시, 8080 직접 X) 주입하고, Metro가 변환결과를 캐시하므로 **`--clear` 필수**. 검증: `grep -rl '{EC2_IP}' dist/_expo` ≥1 & `grep -rl 'localhost:8080' dist/_expo` =0. (gitignore되는 `frontend/.env.production.local`에 `EXPO_PUBLIC_API_URL`을 넣어두면 자동 적용)

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
- **번역/드롭다운 관련**:
  - `src/components/ui/SearchableSelect.tsx` — 검색 드롭다운(학과·국적·선호언어 공용, 라이브러리 없이 Modal+FlatList).
  - `src/data/` — `departments.ts`(경희대 국제캠 학과), `countries.ts`(ISO+다국어, 자동생성), `azureLanguages.ts`(Azure 182언어, 자동생성), `labels.ts`(코드→현지화 라벨 + 레거시 폴백), `selectOptions.ts`.
  - `src/i18n/preferredLanguage.ts` — Azure코드↔버킷 매핑/모드 판별(백엔드 `toBucket`과 일치). `src/hooks/useTextTranslate.ts` — on-demand 번역 토글(캐시).
  - 데이터 재생성 스크립트: `scripts/fetch-languages.ts`·`fetch-countries.ts`·`translate-data.ts`(VI/UZ/MN 보강).

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
| shared.infra → BC 엔티티 | 순수 HTTP는 `AzureTranslateClient`로 분리 완료. 단 `TranslationService`(사전번역 영속화)·`S3Service`는 여전히 board/qna 엔티티 결합 → 추후 분리 |
| 채팅/콘텐츠 on-demand 번역 캐시 | 현재 클라(세션) 캐시만 — 서버측 캐시 테이블 없음. 같은 메시지 재방문 시 재호출. Azure Free F0(월 2M자) 한도 주의 → 수동 탭으로 호출 억제 |
| 멘토링 "같은 언어 +2" | 6개 외 언어 사용자는 버킷이 모두 EN → 서로 동일 언어로 집계되는 미세 부정확(추후 preferredLanguage 기준 보정) |
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
