# Refactor — Bounded Context 격리 (대규모 리팩토링 계획)

> 상태: **진행 중 — Phase 0 완료 + Phase 1 골격/identity 완료** · 최종 갱신: 2026-05-22
> 브랜치: `refactor/bc-isolation`
>
> **진행 현황:**
> - ✅ Phase 0 (안전망): Flyway 도입 + V1 baseline(21테이블) + Testcontainers 하네스 + characterization 테스트 35개 — 커밋 `f563de3`
> - ✅ Phase 1-a: `global` → `shared` 패키지 리네임 (77파일, 동작 불변 검증) — 커밋 `2ab70fd`
> - ✅ **identity BC 재배치 완료** (커밋 7개, 매 단계 `./gradlew test` 35개 그린):
>   - 풀 격리(D7): `@ManyToOne/@OneToOne Member` 참조 12엔티티 전부 `Long ID`로 전환 (Post/PostLike/Comment/CommentLike/QnA/Answer/QnALike/AnswerLike/ChatMessage/MentorMenteeMatch/QuizResult/Profile). 컬럼명 동일 → 스키마 무변경.
>   - 빌더 전달용 `memberRepository.findById` 존재검사 제거 (JWT+FK 보장). 단 chat `receiverId`(클라 입력)만 `existsById` 유지.
>   - `Member`+`MemberRepository`+`auth`(컨트롤러/서비스/DTO8) → `com.khu.globalhub.identity` BC 4계층(domain/application/infrastructure/presentation)으로 `git mv`.
> - ✅ **profile BC 추출 완료** (커밋 2개):
>   - P1: `domain/member`(Profile/ProfileRepository/MemberService/MemberController/DTO3) → `com.khu.globalhub.profile` 4계층으로 `git mv`. 클래스명 유지(순수 이동). 테이블명 불변=스키마 무변경.
>   - P2: identity→profile 결합 제거 — `AuthService`가 `Profile`/`ProfileRepository` 대신 **`ProfileGateway`(DIP 포트, identity가 정의)** 만 의존. 어댑터(`ProfileGatewayAdapter`)는 profile BC가 구현. 프로필 생성 규칙은 profile로 이전. `POST /api/auth/profile`·login `hasProfile` 동작/경로 불변. → **identity는 profile을 import하지 않음**.
> - ✅ **ProfileQueryPort 분리 완료** (커밋 1개): 콘텐츠/채팅 BC(board/qna/comment/chat)가 작성자·발신자 이름 조회로 `ProfileRepository`를 직접 import하던 결합을 **`shared.port.ProfileQueryPort`** (findName + findCard) 계약으로 전환. 구현은 `profile/application/ProfileQueryAdapter`. → 4개 BC에서 profile 패키지 import 완전 제거. (배치 `findNames`는 N+1 최적화 단계로 미룸.)
> - ⏭️ **다음 재개 지점 (택1)**:
>   - (a) **남은 BC 패키지 재배치**: `domain/{board,qna,comment,chat,mentoring,quiz,anonymous}` → 톱레벨 `com.khu.globalhub.{board,qna,chat,mentoring,campusguide,...}` 4계층 (identity/profile과 동일 방식).
>   - (b) **extevent 인프라 + 위반 결합 정리**: `MentoringService`의 chat 직접쓰기(`ChatMessageRepository` INSERT) → `MatchCreatedEvent`, quiz→profile `quizScore` → `QuizCompletedEvent`. mentoring→profile(매칭용 Profile 직접조회)·profile→identity(email) 포트화.
>   - (c) **댓글 board 흡수(D3/D4) + 스키마 V2~V4**: qna/answer 댓글 폐기, target_type·board_type 제거. ⚠️ 동작/프론트 변경 동반.
> - ⏳ 남음: ArchUnit 규칙(task 7) / 남은 BC 재배치 / extevent / 댓글 흡수 / 스키마 정리(V2~V4)
> 목적: 바이브코딩으로 결합된 현재 모놀리식을 **BC 단위로 격리**해서 3인 팀이 각자 자기 영역을 온전히 소유하도록 재구성한다.
> 레퍼런스 아키텍처: `tech-blog-be` (DDD-Lite, 싱글 모듈, 패키지 기반 BC + 통합 이벤트)
>
> ⚠️ 빅뱅(전면 교체) 방식. 진행 중 **신규 기능 동결**. 리팩토링 완료 후 `ARCHITECTURE.md`를 새 구조로 갱신한다.

---

## 0. 배경 — 왜 하는가

현재 구조의 문제 (탐색 결과):

1. **`Member`가 신(神) 엔티티** — Post/QnA/Answer/Comment/Chat/Mentoring/Quiz가 전부 `@ManyToOne Member`로 직접 결합 → 모든 도메인이 하나로 묶임. BC 분리 불가의 1번 원인.
2. **서비스 레벨 경계 위반** — `MentoringService`가 `ChatMessageRepository`로 시스템 메시지 직접 INSERT, `QuizService`가 `profileRepository`로 `quizScore` 직접 UPDATE.
3. **generic Comment** — `targetType(POST/QNA/ANSWER)+targetId` 하나로 다 처리 → 모든 콘텐츠 도메인에 의존하는 허브.

목표: 각 BC가 **자기 테이블/엔티티/규칙만 알고**, 다른 BC는 **ID 또는 이벤트로만** 알게 한다.

---

## 1. 타겟 패키지 구조 + 4계층 규칙

### 1-1. 패키지 레이아웃 (top-level BC)

```
com.khu.globalhub/
├── identity/         # 계정/인증/JWT/비밀번호 (구 auth + Member)
├── profile/          # 개인 프로필 (구 Profile)
├── board/            # 게시판(1종) + 댓글(흡수) + 좋아요/이미지
├── qna/              # 질문 + 답변(1계정1개, 채택) + 좋아요(Q/A 각각)
├── chat/             # DM
├── mentoring/        # 매칭 + 스케줄러
├── campusguide/      # 교내 가이드 맵(정적) + 퀴즈
├── lecturecatalog/   # (신규) 시간표 크롤링 → 강의 데이터 적재
├── coursereview/     # (신규) 강의평
└── shared/           # 전역 — 어떤 BC도 import 하지 않음
    ├── extevent/     # 통합 이벤트 정의
    ├── anonymous/    # 익명번호 (supporting)
    ├── translation/  # 번역 (cross-cutting infra)
    ├── config/       # Security, S3, Async, JPA
    ├── exception/    # ErrorCode, CustomException, GlobalExceptionHandler
    ├── jwt/          # JwtTokenProvider, JwtAuthenticationFilter
    ├── s3/           # S3Service
    └── common/       # BaseTimeEntity, ApiResponse, enums
```

### 1-2. BC 내부 4계층 (의존 방향 단방향)

```
presentation ──┐
               ├──► application ──► domain
infrastructure ┘

  domain         : 엔티티 + 도메인 로직. JPA 애노테이션 OK, Spring 스테레오타입(@Service 등) 금지.
                   같은 BC의 domain만 import.
  application    : 유스케이스(서비스). domain + 자기 BC repository 주입.
  infrastructure : repository, 외부 어댑터. domain/application 알아도 됨.
  presentation   : controller + 요청/응답 DTO. application/domain 알아도 됨.
```

### 1-3. 강제 규칙 (ArchUnit으로 테스트)

- ❌ BC가 다른 BC의 패키지를 import 금지 (`shared` 제외)
- ❌ `shared`가 BC 패키지를 import 금지
- ❌ `domain` 계층이 application/infrastructure/presentation import 금지
- ✅ BC 간 통신은 **ID 참조** 또는 **통합 이벤트**로만

---

## 2. BC 목록 · 소유자 · 경계

| BC | 소유자 | 핵심 엔티티 | 외부 의존(허용된 방식) |
|----|--------|------------|----------------------|
| identity | **본인** | Member, RefreshToken | — (뿌리) |
| profile | **본인** | Profile | identity를 `memberId`로 |
| board | **본인** | Post, PostLike, PostImage, PostTranslation, **Comment(흡수)**, CommentLike, CommentTranslation | identity `authorId`, anonymous, translation |
| qna | **본인** | QnA, Answer, QnALike, AnswerLike, translations | identity `authorId`, anonymous, translation |
| campusguide | **태경님** | 가이드 콘텐츠, QuizQuestion, QuizResult | identity `memberId`, **`QuizCompletedEvent` 발행** |
| chat | **현우님** | ChatMessage | identity `memberId`, **profile 읽기 포트** |
| mentoring | **현우님** | MentorMenteeMatch | identity `memberId`, chat(이벤트, 같은 소유자) |
| lecturecatalog | (추후) | Lecture, 크롤링 적재 | — |
| coursereview | (추후) | Review | identity, lecturecatalog |

> **분담 원칙**: "본인이 가장 많이 수정" → 모두가 의존하는 토대(shared/identity/profile)와 큰 콘텐츠(board/qna)를 본인이 소유. 태경=campusguide(quiz), 현우=mentoring+chat.

---

## 3. 크로스-BC 규칙

### 3-1. 참조: ID만 (엔티티 직접참조 금지)

```java
// AS-IS
@ManyToOne(fetch = LAZY) private Member author;

// TO-BE
@Column(name = "author_id") private Long authorId;   // 컬럼명 동일 → 스키마 무변경
```

작성자 이름 등 부가정보가 필요하면 **service에서 배치 조회 후 DTO 조립** (N+1 주의):

```java
Map<Long,String> names = profileQueryPort.findNames(authorIds); // 배치
```

> **중요**: DB의 외래키(FK) 제약은 **유지**한다. 코드에서 `@ManyToOne`을 빼는 것과 DB FK 삭제는 별개. 단일 DB 모놀리식에서는 FK가 참조 무결성을 지켜주므로 그대로 둔다.

### 3-2. 통신: 통합 이벤트

- 위치: `shared/extevent/<발행BC>/`
- 패턴: `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`
- payload: **ID만** (엔티티/집계값 금지)
- 네이밍: `{주체}{과거형동작}` (예: `QuizCompletedEvent`, `MatchCreatedEvent`)

### 3-3. 크로스-오너 계약 (병렬 작업을 위해 시그니처 선합의)

이 두 인터페이스만 먼저 확정하면 태경/현우는 본인 코드 완성을 안 기다리고 mock으로 병렬 진행 가능.

**① `QuizCompletedEvent`** — 태경(campusguide) 발행 → 본인(profile) 소비
```java
public record QuizCompletedEvent(Long memberId, double score) {}
// profile 리스너가 받아 quizScore 갱신. 태경은 profile 내부를 모름.
```

**② `ProfileQueryPort`** — 본인(profile) 노출 → 현우(chat)가 표시이름 조회
```java
public interface ProfileQueryPort {
    Map<Long, String> findNames(Collection<Long> memberIds);
}
// chat은 이 인터페이스만 의존. profile 엔티티 import 금지.
```

> mentoring → chat 결합은 **둘 다 현우님 소유**가 되어 크로스-오너 계약 불필요(BC 격리상 이벤트로 끊되 협의 비용 0).

---

## 4. 데이터 / 스키마 전략

### 핵심: 풀 격리는 대부분 "코드 변경"이지 "스키마 변경"이 아니다

`@ManyToOne Member`는 이미 DB에서 `*_id` 컬럼이므로, `Long`으로 바꿔도 **매핑 컬럼 동일 → 기존 데이터 그대로 산다.** 패키지 이동도 `@Table(name=...)`만 유지하면 스키마 0 변경.

### 실제로 바뀌는 스키마 (이게 전부)

| 변경 | 영향 |
|------|------|
| `comments`에서 `target_type IN (QNA, ANSWER)` 행 삭제 | ⚠️ 의도된 데이터 삭제 (qna 댓글 폐기) |
| `target_type` 컬럼 제거 (댓글=board 전용) | 컬럼 제거 |
| `posts.board_type` 컬럼 제거 (게시판 1종) | 컬럼 제거 |
| quiz 테이블 → campusguide 소속 | 테이블명 유지 → 영향 없음 |

### 마이그레이션 도구: Flyway 도입 (ddl-auto 졸업)

```
V1__baseline.sql              # 현재 스키마 스냅샷 (운영 RDS 기준)
V2__drop_qna_answer_comments.sql
V3__drop_comment_target_type.sql
V4__drop_post_board_type.sql
```

- 로컬 도커 DB와 운영 RDS에 **동일 SQL이 순서대로** 적용됨 → "기존 DB들"(복수) 일관 처리.
- 운영은 계속 `validate` 유지(Flyway가 스키마를 관리).

### 안전 장치 (= "안전하게 고쳐버려"의 실제 보증)

1. **운영 적용 전 RDS 수동 스냅샷** — 뭐가 터져도 롤백. (t3.micro라 비용·시간 미미)
2. **스냅샷을 로컬 도커로 복원** → 실데이터 복제본 위에서 리팩토링 앱 스모크 테스트 (staging 대용).
3. 로컬 검증 통과 후에만 운영 적용.

---

## 5. 검증 전략 ⭐ (빅뱅의 안전망)

테스트 없는 상태로 갈아엎으면 도박. **리팩토링 전에 현재 동작을 테스트로 박제(characterization test)** → 리팩토링 후 그대로 통과하면 "행동 안 바뀜" 보장.

```
1) [리팩토링 前] 핵심 경로 통합테스트 — Testcontainers + MockMvc
   대상: 인증 플로우 / 게시글 작성·조회·댓글 / qna 답변·채택 /
        좋아요 토글 / 채팅 / 멘토링 매칭 / 퀴즈 점수
   → 현재 응답을 골든 기준으로 캡처
2) [리팩토링 中] 각 BC 이전마다 위 테스트 그린 유지
   특히 ID참조 전환 후 authorName 등이 동일하게 나오는지가 핵심
3) [운영 反영 前] 스냅샷→로컬 복원본 위 스모크 테스트
4) [운영] 스냅샷 → Flyway → 배포
```

> 풀 커버리지는 과함. **바뀌는 부분**(작성자 조회 / 댓글 제거 / boardType 제거 / 이벤트 2개) 위주 happy path + 비즈니스 룰(채택 금지 등)만.

### 외부 API 계약은 고정 (프론트 충격 최소화)

내부를 ID참조로 바꿔도 **응답 DTO 그대로 유지**(`authorName` 등 살아있음). 프론트가 바꿀 것은 **딱 2개**:
- ❌ qna/답변 댓글 API 제거
- ❌ 게시판 `boardType` 파라미터 제거

---

## 6. 작업 순서 (Phase)

```
Phase 0  세팅 [본인]      : RDS 스냅샷 / Flyway baseline(V1) / 테스트 하네스 / 이 문서
Phase 1  골격 [본인]      : shared/ + top-level BC 패키지 + 4계층 스캐폴딩 + ArchUnit → 즉시 머지
Phase 2  토대 [본인] ⭐    : identity → profile + extevent 인프라
         ───────────────  여기까지 머지되면 태경/현우 풀림 ───────────────
Phase 3  콘텐츠 [본인]    : board(글+댓글 흡수) / qna(답변만)
Phase 3' 병렬 [태경/현우] : campusguide(quiz) / mentoring+chat
Phase 4  스키마 정리      : Flyway V2~V4 (qna댓글 삭제 / target_type·board_type 제거)
Phase 5  신규 [추후]      : lecturecatalog → coursereview
```

> ⚠️ **본인이 크리티컬 패스.** 태경/현우 BC가 전부 identity/profile에 의존하므로, **Phase 0→1→2(identity/profile)를 최우선으로 빨리 빼서 머지**해야 팀 전체가 막히지 않는다. 그 다음 본인 board/qna 진행.

### 브랜치 / 협업
- `refactor/bc-isolation` 장기 브랜치, 진행 중 신규 기능 동결.
- Phase 1 골격은 **본인이 빠르게 만들어 즉시 머지** → 나머지가 그 위에서 분기.
- 이후 BC 단위로 PR 쪼개 자주 머지 (long-lived 충돌 회피).

---

## 7. 결정 로그

| # | 결정 | 비고 |
|---|------|------|
| D1 | 빅뱅 전면 교체 (점진 X) | "초대규모 리팩토링 OK" |
| D2 | 게시판 3종(신입/자유/졸업) → 1종 | boardType 제거, 프론트 [자유게시판\|QnA] 2탭 |
| D3 | QnA는 답변만, **댓글 제거** | generic Comment 폐기, 댓글은 board 전용으로 흡수 |
| D4 | 댓글은 board BC가 소유 (모델 2) | CommentTargetType enum 제거 |
| D5 | quiz는 독립 BC 아님 → campusguide 소속 | 점수만 profile로 이벤트 |
| D6 | 강의평은 lecturecatalog(크롤러) / coursereview(리뷰) 분리 | 신규, 추후 |
| D7 | 크로스-BC = ID 참조 only, `@ManyToOne` 금지 | 풀 격리 |
| D8 | BC 통신 = `@TransactionalEventListener(AFTER_COMMIT)`, payload ID만 | |
| D9 | DB FK 제약은 유지 (코드 격리 ≠ FK 삭제) | 단일 DB 모놀리식 |
| D10 | Flyway 도입, ddl-auto 졸업 | 운영은 validate 유지 |
| D11 | 검증 = 리팩토링 전 characterization test (Testcontainers) | |

---

## 8. 미해결 / 후속 논의

- [ ] 크로스-오너 계약 2개(`QuizCompletedEvent`, `ProfileQueryPort`) 최종 시그니처 팀 확정
- [ ] characterization test 커버 범위 합의 (어디까지 잡을지)
- [ ] lecturecatalog 크롤링 대상/주기 (KHU 시간표) 스펙
- [ ] 프론트 변경(댓글 API·boardType 제거) 일정 조율
- [ ] 리팩토링 완료 후 `ARCHITECTURE.md` 전면 갱신
