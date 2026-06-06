# KHU Global Hub — 경희대 유학생 특화 커뮤니티 앱

> 경희대학교 유학생들의 정보 격차를 해소하고, 캠퍼스 적응을 돕는 유학생 전용 커뮤니티 플랫폼

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택](#2-기술-스택)
3. [핵심 기능](#3-핵심-기능)
4. [로컬 개발 환경](#4-로컬-개발-환경)
5. [디렉토리 구조](#5-디렉토리-구조)
6. [API 목록](#6-api-목록)
7. [프론트엔드 개발 가이드](#7-프론트엔드-개발-가이드)
8. [보류 항목](#8-보류-항목)
9. [환경변수 목록](#9-환경변수-목록)

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **프로젝트명** | KHU Global Hub |
| **대상** | 경희대학교 유학생 |
| **플랫폼** | Android 앱 + Web |
| **백엔드** | Spring Boot 3.4.5 + PostgreSQL |
| **프론트엔드** | React Native (Expo) + TypeScript |
| **현재 상태** | 백엔드 완료 · AWS 운영 배포 중 / 프론트엔드 핵심 기능 완료 · 웹 배포 중 |

---

## 2. 기술 스택

### 백엔드

| 항목 | 기술 |
|------|------|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.4.5 |
| ORM | Spring Data JPA (Hibernate) |
| 인증 | Spring Security + JWT (jjwt 0.12.6) |
| DB | PostgreSQL 17 (로컬: Docker, 포트 5433) + Flyway 마이그레이션 |
| 파일 저장 | AWS S3 SDK v2 (2.29.52) |
| 번역 | Microsoft Azure Translator API |
| API 문서 | springdoc-openapi 2.8.5 (Swagger) |
| 빌드 | Gradle |

### 프론트엔드

| 항목 | 기술 |
|------|------|
| 프레임워크 | Expo SDK 52 + React Native |
| 언어 | TypeScript |
| 라우팅 | expo-router (파일 기반) |
| 상태관리 | Zustand |
| HTTP | axios |
| 타겟 플랫폼 | Android + Web |

---

## 3. 핵심 기능

- **다국어 게시판/Q&A** — 작성 시 6개 언어(한·영·중·베트남·**우즈벡**·몽골)로 사전 번역. 6개 외 언어 사용자는 원문 + **on-demand 번역**(`POST /api/translate`, "번역하기" 토글). (선호언어는 Azure 182개 코드로 저장, 6개 버킷 파생)
- **강의평 (coursereview)** — 경희대 국제캠 **실제 강의 2141건**(2026-1) + 익명 별점·리뷰 + 수업지표(출석방식·발표·조모임·과제·한국어사용 빈도, 강의 단위 집계). 강의평 본문도 게시판식 다국어 번역.
- **익명 번호 시스템** — 게시글/Q&A별 독립 익명 컨텍스트, 작성자=익명1 → 이후 댓글 익명2, 3, ...
- **Q&A 채택 시스템** — 질문에 답변을 달고 질문자가 채택, 채택 후 추가 답변 불가
- **멘토-멘티 자동 매칭** — 매년 3월/9월 1일 스케줄러로 점수제 매칭 + 활동기록, 시스템 메시지 발송
- **1:1 DM 채팅** — 자유/멘토링 메시지, 읽음 처리, **메시지 삭제(본인)**, 이미지·파일 첨부, 상대 프로필 표시
- **파일 업로드** — 게시글·프로필·댓글·채팅에 이미지 + **일반 파일 첨부**, AWS S3 비동기
- **게시글 수정** — 작성자 본인 수정(재번역 포함) · **이메일 인증**(@khu.ac.kr) · 비밀번호 재설정

---

## 4. 로컬 개발 / 실행

> 로컬 풀스택 실행 · 환경 설정 · **테스트 계정** 등 상세는 → **[`localtest.md`](./localtest.md)**

요약: `docker compose up -d` → `./gradlew bootRun` (또는 원클릭 `dev.ps1`/`dev.sh`) → 프론트 `npm run web`.
- 로그인은 자동 시드 계정 사용 (회원가입 불필요) — 자세히는 `localtest.md`.
- 프론트 기본 API 주소 = 로컬 백엔드 `http://localhost:8080`.
- 운영 배포 절차는 [`ARCHITECTURE.md`](./ARCHITECTURE.md) §7.

---

## 5. 디렉토리 구조

```
design_thinking/
├── backend/                            # Spring Boot 백엔드
│   ├── src/
│   ├── build.gradle
│   ├── docker-compose.yml
│   └── README.md
└── frontend/                           # Expo React Native 프론트엔드
    ├── app/
    │   ├── (auth)/                     # 로그인, 회원가입, 이메일인증, 프로필설정
    │   └── (main)/                     # 탭 기반 메인 화면
    │       ├── _layout.tsx             # 탭바 설정
    │       ├── index.tsx               # 게시판 목록
    │       ├── board/[postId].tsx      # 게시글 상세 + 댓글
    │       ├── board/create.tsx        # 게시글 작성
    │       ├── qna/                    # Q&A 목록 + 상세 + 작성
    │       ├── chat/                   # 채팅 목록 + 상세
    │       ├── mentoring.tsx           # 멘토링 매칭 정보
    │       └── profile.tsx             # 내 프로필
    ├── src/
    │   ├── api/                        # boardApi, qnaApi, authApi, ...
    │   ├── store/                      # authStore (Zustand)
    │   ├── types/                      # TypeScript 타입 정의
    │   └── components/                 # 공용 컴포넌트 (Screen 등)
    └── constants/
        └── theme.ts                    # Colors, Typography, Spacing, Radius, Shadow

backend/src/main/java/com/khu/globalhub/
├── KhuGlobalHubApplication.java
├── identity/        # 계정·인증·JWT (Member, auth)            [소유: 본인]
├── profile/         # 프로필 (Profile, /api/members)          [소유: 본인]
├── board/           # 게시판(자유 1종) + 댓글(흡수) + 좋아요/이미지  [소유: 본인]
├── qna/             # 질문 + 답변(채택) + 좋아요                [소유: 본인]
├── chat/            # 1:1 DM (삭제·첨부·상대프로필)             [소유: 현우]
├── mentoring/       # 멘토-멘티 매칭 + 스케줄러 + 활동기록       [소유: 현우]
├── campusguide/     # 퀴즈 (+ 학사 가이드 예정)               [소유: 태경]
├── coursereview/    # 강의평 (강의 카탈로그 + 익명 리뷰 + 지표)  [소유: 본인]
├── translation/     # on-demand 번역 (POST /api/translate, 무상태) [소유: 본인]
├── media/           # 파일 업로드 (POST /api/images → S3, 무상태) [소유: 본인]
├── devsupport/      # 로컬/운영 시드 데이터 (@Profile local,prod)
└── shared/          # 전역 공통 — 어떤 BC도 import 하지 않음
    ├── port/        # 크로스-BC 계약(인터페이스): ProfileQueryPort, MemberQueryPort, ProfileGateway
    ├── extevent/    # 통합 이벤트: QuizCompletedEvent(campusguide→profile), MatchCreatedEvent(mentoring→chat)
    ├── anonymous/   # 익명 번호 (supporting)
    ├── config/ exception/ jwt/ infra/ common/ enums/ util/

# 각 BC 내부는 4계층: domain(엔티티·규칙) / application(서비스·리스너) /
#                      infrastructure(리포지토리·어댑터) / presentation(컨트롤러·DTO)
# 자세한 협업 규칙은 아래 "10. BC 협업 가이드" 및 docs/refactor-bc-isolation.md 참고.
```

---

## 6. API 목록

### Auth — `/api/auth`

| Method | Path | 설명 |
|--------|------|------|
| POST | `/register` | 회원가입 (@khu.ac.kr 이메일 인증 코드 발송) |
| POST | `/verify-email` | 이메일 인증 코드 확인 + JWT 발급 |
| POST | `/profile` | 최초 프로필 생성 (신입생=MENTEE 강제) |
| POST | `/login` | 로그인 (accessToken + refreshToken + hasProfile) |
| POST | `/refresh` | 리프레시 토큰으로 액세스 토큰 갱신 |
| POST | `/logout` | 로그아웃 |
| POST | `/forgot-password` | 비밀번호 재설정 코드 발송 (10분) |
| POST | `/reset-password` | 코드 검증 후 새 비번 적용 (전 기기 로그아웃) |

### Member — `/api/members`

| Method | Path | 설명 |
|--------|------|------|
| GET | `/me` | 내 프로필 조회 |
| PUT | `/me` | 내 프로필 수정 |
| PATCH | `/me/mentoring-role` | 멘토링 역할 변경 |
| PATCH | `/me/profile-image` | 프로필 이미지 업로드 (multipart/form-data) |
| GET | `/{memberId}` | 타인 프로필 조회 |
| GET | `/{memberId}/posts` | 특정 멤버 게시글 목록 |

### Board — `/api/posts`

| Method | Path | 설명 |
|--------|------|------|
| POST | `/` | 게시글 작성 (multipart/form-data, 이미지 가능) — boardType 없음(게시판 1종) |
| GET | `/?language=KO` | 게시글 목록 (페이징, 게시판 1종) |
| GET | `/popular?language=KO` | 인기 게시물 (좋아요 10개 이상) |
| GET | `/{postId}?language=KO` | 게시글 상세 (isLiked, isOwner 포함) |
| PUT | `/{postId}` | 게시글 수정 (multipart, 작성자만 — 재번역 + 이미지 추가) |
| DELETE | `/{postId}` | 게시글 삭제 (작성자만) |
| POST | `/{postId}/like` | 좋아요 토글 |

> 읽기 엔드포인트(`/`, `/popular`, `/{postId}`, 댓글)는 `&original=true` 지원 — 번역본 대신 원문 행 반환(6개 외 언어 사용자용).

### Comment

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/posts/{postId}/comments` | 게시글 댓글 작성 (parentId 있으면 대댓글) |
| GET | `/api/posts/{postId}/comments?language=KO` | 댓글 목록 (대댓글 포함) |
| DELETE | `/api/comments/{commentId}` | 댓글 삭제 |
| POST | `/api/comments/{commentId}/like` | 댓글 좋아요 토글 |

> 댓글은 **게시글 전용**입니다. (D3: QnA·답변 댓글 API는 폐기됨)

### Q&A — `/api/qnas`

| Method | Path | 설명 |
|--------|------|------|
| POST | `/` | 질문 작성 |
| GET | `/?language=KO` | 질문 목록 (페이징) |
| GET | `/{qnaId}?language=KO` | 질문 상세 + 답변 목록 |
| DELETE | `/{qnaId}` | 질문 삭제 (작성자만) |
| POST | `/{qnaId}/like` | 질문 좋아요 토글 |
| POST | `/{qnaId}/answers` | 답변 작성 (채택 후 불가, 본인 질문 불가, 1인 1답) |
| DELETE | `/{qnaId}/answers/{answerId}` | 답변 삭제 (작성자만) |
| POST | `/{qnaId}/answers/{answerId}/adopt` | 답변 채택 (질문 작성자만, 1회) |
| POST | `/{qnaId}/answers/{answerId}/like` | 답변 좋아요 토글 |

### Mentoring — `/api/mentoring`

| Method | Path | 설명 |
|--------|------|------|
| GET | `/me` | 내 현재 ACTIVE 매칭 정보 (상대방 프로필 포함) |
| GET | `/me/history` | 내 전체 매칭 이력 |
| GET·POST | `/{matchId}/activities` | 멘토링 활동 기록 조회/작성 (매칭 당사자만) |
| POST | `/run` | 수동 매칭 트리거 — **로컬 전용**(`@Profile("local")`) |

### Chat — `/api/chat`

| Method | Path | 설명 |
|--------|------|------|
| POST | `/` | 메시지 전송 (텍스트/이미지/파일) |
| GET | `/` | DM 목록 (대화 상대 + 마지막 메시지 + 안읽은 수) |
| GET | `/{partnerId}` | 특정 상대와 대화 내용 조회 + 읽음 처리 |
| DELETE | `/{messageId}` | 메시지 삭제 (본인이 보낸 것만) |

### Translation — `/api/translate`

| Method | Path | 설명 |
|--------|------|------|
| POST | `/` | on-demand 번역 `{texts[], target, source?}` → `{translations[], detectedSource}` |

### Media — `/api/images`

| Method | Path | 설명 |
|--------|------|------|
| POST | `/` | 파일 업로드(multipart `image`, 이미지 외 파일도 허용) → S3 → `{url}` |

### Course Review — `/api/lectures`

| Method | Path | 설명 |
|--------|------|------|
| GET | `/?semester=2026-1&query=&language=KO` | 강의 검색 (이름/교수/코드 LIKE, 페이징) |
| GET | `/{lectureId}` | 강의 상세 + 지표 집계 + 강의평 목록(익명) |
| POST | `/{lectureId}/reviews` | 강의평 작성 (별점·본문 필수, 지표 5종 선택) |
| DELETE | `/reviews/{reviewId}` | 강의평 삭제 (작성자 본인만) |

---

## 7. 프론트엔드 개발 가이드

### 공통 요청 규칙

**인증 헤더** (로그인 후 모든 요청에 필수):
```
Authorization: Bearer {accessToken}
```

**공통 응답 형식**:
```json
{
  "success": true,
  "message": "ok",
  "data": { ... }
}
```
실패 시: `success: false`, `data: null`, `message`에 에러 내용.

**언어 파라미터**: `?language=KO` (KO/EN/ZH/VI/UZ/MN — 한/영/중/베트남/우즈벡/몽골), 생략 시 기본 KO. 6개 외 언어 사용자는 `&original=true` + `POST /api/translate`.

**페이지네이션**: `?page=0&size=20` (Spring Pageable 기본값).

---

### 인증 플로우

```
1. POST /api/auth/register        → 이메일 발송
2. POST /api/auth/verify-email    → accessToken + refreshToken + hasProfile
3. hasProfile=false 이면 →
   POST /api/auth/profile         → 프로필 생성
4. 이후 모든 요청: Authorization: Bearer {accessToken}
5. 401 응답 시 → POST /api/auth/refresh 로 토큰 갱신
```

- accessToken 유효기간: **24시간**
- refreshToken 유효기간: **7일** (DB 저장, 로그아웃 시 무효화)

---

### 주요 응답 필드

**게시글 목록 (`PostSummaryResponse`)**
```
postId, boardType, title, authorName (익명이면 "익명N"),
likeCount, commentCount, createdAt, isAnonymous
```

**게시글 상세 (`PostDetailResponse`)**
```
위 필드 + content, imageUrls[], isLiked, isOwner
```

**댓글 (`CommentResponse`)**
```
commentId, content, authorName (익명이면 "익명N"),
likeCount, isLiked, isOwner, createdAt,
children[] (대댓글, 동일 구조)
```

**Q&A 목록 (`QnASummaryResponse`)**
```
qnaId, title, authorName, likeCount, answerCount, isAdopted, createdAt
```

**답변 (`AnswerResponse`)**
```
answerId, content, authorName (익명이면 "익명N"),
likeCount, isLiked, isOwner, isAdopted, createdAt
```

**멘토링 매칭 (`MentoringMatchResponse`)**
```
matchId, semester, status, myRole (MENTOR/MENTEE),
matchedAt, partner (ProfileResponse)
```

**DM 목록 (`ConversationSummaryResponse`)**
```
partnerId, partnerName, partnerProfileImage,
lastMessage, unreadCount, lastMessageAt
```

---

### Android HTTP 설정

백엔드가 HTTP로 운영 중이므로 `AndroidManifest.xml`에 다음 설정 필요:
```xml
<application
  android:usesCleartextTraffic="true"
  ...>
```

---

## 8. 보류 항목

| 항목 | 비고 |
|------|------|
| 학사 가이드 백엔드 | 팀 자료조사 완료 후 별도 브랜치 |
| 학사 퀴즈 | 가이드 완성 후 연동 |
| Q&A 채택 마일리지 | 채택 기능 구현 완료, 마일리지는 추후 |
| 게시글 신고/블라인드 | Phase 2 |
| 푸시 알림 | Phase 2 |
| 댓글 수정 API | 게시글 수정은 구현 완료(PUT), 댓글 수정은 미구현 |
| 채팅 서버측 번역 캐시 | 현재 클라(세션) 캐시만 — Azure Free F0 한도 주의 |
| APK 재빌드 | 최신 프론트 코드 반영 필요 |

---

## 9. 환경변수 목록

운영 배포 시 필요한 환경변수:

```
JWT_SECRET
AZURE_TRANSLATOR_KEY
AZURE_REGION
AWS_S3_BUCKET
AWS_S3_REGION
AWS_ACCESS_KEY
AWS_SECRET_KEY
MAIL_USERNAME
MAIL_PASSWORD
DB_URL
DB_USERNAME
DB_PASSWORD
DDL_AUTO
```

---

## 10. BC 협업 가이드 (Bounded Context 격리 후)

> 백엔드는 **BC(Bounded Context) 단위로 격리**되어 있어, 각자 자기 영역만 수정하면 충돌이 거의 없습니다.
> 설계 배경·결정 로그는 [`docs/refactor-bc-isolation.md`](../Lim/refactor-bc-isolation.md), 아키텍처 전반은 [`ARCHITECTURE.md`](ARCHITECTURE.md).

### 10-1. 소유권 (누가 뭘 만지나)

| BC | 소유자 | 패키지 | 핵심 |
|----|--------|--------|------|
| identity | 본인 | `identity` | Member, 회원가입/로그인/JWT/비번재설정 |
| profile | 본인 | `profile` | Profile, `/api/members` |
| board | 본인 | `board` | 게시글(자유 1종)·댓글·좋아요·이미지 |
| qna | 본인 | `qna` | 질문·답변·채택·좋아요 |
| **chat** | **현우** | `chat` | 1:1 DM |
| **mentoring** | **현우** | `mentoring` | 매칭·스케줄러 |
| **campusguide** | **태경** | `campusguide` | 퀴즈 (+ 학사 가이드 예정) |
| shared | 공용 | `shared` | 설정·예외·JWT·포트·이벤트·익명번호 등 |

### 10-2. 황금 규칙

1. **자기 BC 패키지 안에서만 작업** — `com.khu.globalhub.<내BC>` 안에서 4계층(domain/application/infrastructure/presentation)으로.
2. **다른 BC 패키지를 절대 import 하지 않는다** (`shared` 제외). 다른 BC의 엔티티·리포지토리를 직접 쓰면 ❌.
3. **다른 BC가 필요하면 셋 중 하나**:
   - **ID 참조** — 남의 엔티티를 `@ManyToOne` 하지 말고 `Long xxxId` 컬럼으로만 가진다.
   - **shared 포트** — 읽기/호출이 필요하면 `shared.port`의 인터페이스로 (구현은 소유 BC가 제공).
   - **통합 이벤트** — 다른 BC에 부수효과를 일으켜야 하면 `shared.extevent` 이벤트 발행(`@TransactionalEventListener(AFTER_COMMIT)`으로 소비).
4. 이 규칙은 **ArchUnit으로 강제**됩니다(`BoundedContextRulesTest`). 어기면 빌드 실패 → 바로 알 수 있음.

### 10-3. 이미 만들어진 크로스-BC 계약 (그대로 쓰세요)

`shared.port` (인터페이스 — 호출만, 구현은 신경 X):
```java
ProfileQueryPort   findName(memberId) / findCard(memberId)   // 표시 이름·프로필이미지 (profile이 구현)
MemberQueryPort    exists(memberId) / findEmail(memberId)     // 계정 존재·이메일 (identity가 구현)
ProfileGateway     exists(memberId) / create(command)         // 프로필 존재·생성 (profile이 구현)
```
`shared.extevent` (통합 이벤트):
```java
QuizCompletedEvent(memberId, score)   // campusguide 발행 → profile이 quizScore 갱신
MatchCreatedEvent(mentorId, menteeId) // mentoring 발행 → chat이 시스템 메시지 삽입
```
예) 현우님이 매칭 만들 때 채팅에 직접 INSERT ❌ → `eventPublisher.publishEvent(new MatchCreatedEvent(...))` 만 하면 chat이 알아서 처리.
예) 태경님이 퀴즈 점수를 프로필에 반영 ❌ 직접 UPDATE → `QuizCompletedEvent` 발행만.

### 10-4. "다른 BC 데이터가 필요해요" 패턴

- **표시 이름/이메일 같은 단순 읽기** → 위 포트(`ProfileQueryPort`/`MemberQueryPort`) 사용. 없는 메서드가 필요하면 `shared.port`의 인터페이스에 추가하고 소유 BC에 구현(`@Component implements ...Port`)을 넣으면 됨.
- **남의 상태를 바꿔야 함** → 이벤트 발행. payload엔 **ID(+단순 값)만** 담는다(엔티티 금지).
- 절대 남의 `Repository`를 주입하지 말 것.

### 10-5. 새 기능 추가 절차 (예: campusguide에 가이드 화면)

1. `campusguide/domain`에 엔티티, `infrastructure`에 리포지토리, `application`에 서비스, `presentation`에 컨트롤러·DTO.
2. 회원 정보가 필요하면 `MemberQueryPort`/`ProfileQueryPort` 주입.
3. 스키마 변경은 **Flyway 마이그레이션**으로만 (`src/main/resources/db/migration/V{n}__설명.sql`). `ddl-auto`는 운영=validate.
4. `./gradlew test` — characterization + ArchUnit 그린 확인 후 PR.

### 10-6. 안전망 / 검증

```bash
./gradlew test     # characterization(동작 박제) + ArchUnit(경계) — 약 2분
```
- Testcontainers로 실제 PostgreSQL 17 + Flyway(V1~)로 스키마 생성 → 운영과 동일 환경 검증.
- 리팩토링·기능 추가 후 이 테스트가 그린이면 "기존 동작 안 깨짐 + 경계 안 무너짐"이 보장됨.

### 10-7. 알아둘 잔여 결합 (점진 개선 중, 새로 만들 땐 따라하지 말 것)

- `mentoring → profile` (매칭 알고리즘이 Profile 직접 조회) — 추후 포트화 예정.
- `shared.infra`(TranslationService/S3Service) `→ board/qna 엔티티` — 번역·이미지 영속화 결합, 추후 분리 예정.

> ⚠️ **계약 변경 이력(프론트 영향)**: 게시판은 1종(자유)으로 통합되어 `boardType` 파라미터·필드가 사라졌고, QnA·답변 댓글 API는 폐기되었습니다. 댓글은 게시글 전용입니다.

---

*경희대학교 디자인씽킹 팀프로젝트 — 2026*
