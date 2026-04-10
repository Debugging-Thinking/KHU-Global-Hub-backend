# KHU Global Hub — 경희대 유학생 특화 커뮤니티 앱

> 경희대학교 유학생들의 정보 격차를 해소하고, 캠퍼스 적응을 돕는 유학생 전용 커뮤니티 플랫폼

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택](#2-기술-스택)
3. [로컬 개발 환경 실행](#3-로컬-개발-환경-실행)
4. [디렉토리 구조](#4-디렉토리-구조)
5. [도메인 설계 핵심 결정사항](#5-도메인-설계-핵심-결정사항)
6. [구현된 API 목록](#6-구현된-api-목록)
7. [미구현 / 다음 세션 작업 목록](#7-미구현--다음-세션-작업-목록)
8. [보류 항목](#8-보류-항목)
9. [환경변수 가이드](#9-환경변수-가이드)

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **프로젝트명** | KHU Global Hub |
| **대상** | 경희대학교 유학생 |
| **플랫폼** | iOS / Android (Flutter) |
| **백엔드** | Spring Boot 3.4.5 + PostgreSQL |
| **현재 상태** | 백엔드 빌드 성공, 가이드 기능 제외 전 기능 구현 완료 |

### 핵심 기능 요약
- 6개 언어 자동 번역 게시판 (Azure Translator, 작성 시 사전 번역)
- Q&A 채택 시스템
- 멘토-멘티 자동 매칭 스케줄러 (매년 3월/9월 1일)
- 1:1 DM 채팅 (폴링 방식, 시스템 메시지 포함)
- AWS S3 이미지 업로드 (비동기)

---

## 2. 기술 스택

| 항목 | 기술 |
|------|------|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.4.5 |
| ORM | Spring Data JPA (Hibernate) |
| 인증 | Spring Security + JWT (jjwt 0.12.6) |
| DB | PostgreSQL 16 (로컬: Docker) |
| 파일 저장 | AWS S3 SDK v2 (2.29.52) |
| 번역 | Microsoft Azure Translator API |
| API 문서 | springdoc-openapi 2.8.5 (Swagger) |
| 빌드 | Gradle |

---

## 3. 로컬 개발 환경 실행

**1. DB 실행**
```bash
docker compose up -d
```
- host: localhost:5432
- db: khu_global_hub / user: globalhub / pw: globalhub1234

**2. Spring Boot 실행**
```bash
./gradlew bootRun
```

**3. Swagger UI**
```
http://localhost:8080/swagger-ui/index.html
```

> **GitHub 업로드 전 주의**: `application.yml`에 로컬 DB 비밀번호가 하드코딩 되어있음.
> 운영 배포 시 `application-prod.yml` 분리 + `.gitignore` 처리 필요.
> JWT_SECRET, AZURE_TRANSLATOR_KEY, AWS 키는 이미 환경변수로 처리됨.

---

## 4. 디렉토리 구조

```
globalhub/                         # 프로젝트 루트 (백엔드 + 배포)
├── frontend/                      # Flutter 프론트엔드 (Android Studio)
├── src/
├── build.gradle
├── docker-compose.yml
└── README.md

src/main/java/com/khu/globalhub/
├── KhuGlobalHubApplication.java
│
├── global/
│   ├── common/
│   │   ├── ApiResponse.java          # 통일 응답 래퍼 {success, message, data}
│   │   └── BaseTimeEntity.java       # createdAt, updatedAt (JPA Auditing)
│   ├── config/
│   │   ├── AsyncConfig.java          # @EnableAsync + @EnableScheduling
│   │   │                             # translationExecutor(4-8) + s3Executor(2-4)
│   │   ├── JpaConfig.java            # @EnableJpaAuditing
│   │   ├── S3Config.java             # S3Client Bean
│   │   └── SecurityConfig.java       # STATELESS JWT, permitAll 경로 설정
│   ├── enums/
│   │   ├── Language.java             # KO/EN/ZH/VI/ES/MN + toAzureCode()
│   │   ├── BoardType.java            # FRESHMAN/FREE/GRADUATE
│   │   ├── CommentTargetType.java    # POST/QNA/ANSWER
│   │   ├── MentoringRole.java        # MENTOR/MENTEE/NONE
│   │   └── MatchStatus.java          # ACTIVE/COMPLETED/CANCELLED
│   ├── exception/
│   │   ├── ErrorCode.java            # 전체 에러코드 enum (HTTP status + message)
│   │   ├── CustomException.java
│   │   └── GlobalExceptionHandler.java
│   ├── infra/
│   │   ├── TranslationService.java   # @Async Azure Translator 호출
│   │   │                             # translatePost / translateComment
│   │   │                             # translateQnA / translateAnswer
│   │   └── S3Service.java            # @Async S3 업로드 (게시글 이미지)
│   ├── jwt/
│   │   ├── JwtTokenProvider.java     # 토큰 생성/검증 (jjwt 0.12.6)
│   │   └── JwtAuthenticationFilter.java  # principal = Long memberId
│   └── util/
│       └── SecurityUtil.java         # getCurrentMemberId()
│
└── domain/
    ├── auth/
    │   ├── controller/AuthController.java
    │   ├── service/AuthService.java
    │   ├── service/EmailService.java
    │   └── dto/  (RegisterRequest, VerifyEmailRequest, CreateProfileRequest,
    │              LoginRequest, LoginResponse, RefreshRequest)
    │
    ├── member/
    │   ├── entity/
    │   │   ├── Member.java           # 인증 전용 (email, password, refreshToken 등)
    │   │   └── Profile.java          # 프로필 (name, department, nationality,
    │   │                             #          admissionYear, language, mentoringRole)
    │   ├── repository/
    │   │   ├── MemberRepository.java
    │   │   └── ProfileRepository.java
    │   ├── service/MemberService.java
    │   ├── controller/MemberController.java
    │   └── dto/  (ProfileResponse, UpdateProfileRequest, UpdateMentoringRoleRequest)
    │
    ├── board/
    │   ├── entity/  (Post, PostTranslation, PostImage, PostLike)
    │   ├── repository/  (PostRepository, PostTranslationRepository,
    │   │                 PostLikeRepository, PostImageRepository)
    │   ├── service/PostService.java
    │   ├── controller/PostController.java
    │   └── dto/  (CreatePostRequest, PostSummaryResponse, PostDetailResponse)
    │
    ├── comment/
    │   ├── entity/  (Comment, CommentTranslation, CommentLike)
    │   ├── repository/  (CommentRepository, CommentTranslationRepository,
    │   │                 CommentLikeRepository)
    │   ├── service/CommentService.java
    │   ├── controller/CommentController.java   # /api/posts/{postId}/comments
    │   └── dto/  (CreateCommentRequest, CommentResponse)
    │
    ├── qna/
    │   ├── entity/  (QnA, QnATranslation, QnALike,
    │   │             Answer, AnswerTranslation, AnswerLike)
    │   ├── repository/  (QnARepository, QnATranslationRepository, QnALikeRepository,
    │   │                 AnswerRepository, AnswerTranslationRepository, AnswerLikeRepository)
    │   ├── service/QnAService.java
    │   ├── controller/QnAController.java
    │   └── dto/  (CreateQnARequest, CreateAnswerRequest,
    │              QnASummaryResponse, QnADetailResponse, AnswerResponse)
    │
    ├── mentoring/
    │   ├── entity/MentorMenteeMatch.java
    │   ├── repository/MentorMenteeMatchRepository.java
    │   ├── service/MentoringService.java
    │   └── scheduler/MentoringScheduler.java   # 3월/9월 1일 cron
    │
    └── chat/
        ├── entity/ChatMessage.java
        ├── repository/ChatMessageRepository.java
        ├── service/ChatService.java
        ├── controller/ChatController.java
        └── dto/  (SendMessageRequest, ChatMessageResponse, ConversationSummaryResponse)
```

---

## 5. 도메인 설계 핵심 결정사항

다음 세션 AI가 반드시 숙지해야 할 설계 결정들.

### 5-1. Member / Profile 분리
- `members` 테이블: 인증 전용 (email, password, refreshToken 등)
- `profiles` 테이블: 프로필 데이터 (name, department, nationality, admissionYear, language, mentoringRole)
- Profile row가 존재하면 프로필 완성, 없으면 미완성
- `LoginResponse`에 `hasProfile` 플래그 → 프론트가 프로필 생성 화면으로 리다이렉트

### 5-2. JWT 구조
- Access token: 24시간 (stateless, DB 미저장)
- Refresh token: 7일 (DB 저장 — `members.refresh_token`)
- `SecurityContext`의 principal = `Long memberId` (UsernamePasswordAuthenticationToken)
- `SecurityUtil.getCurrentMemberId()` 패턴으로 현재 유저 ID 조회

### 5-3. 번역 비동기 처리
- 글/댓글 작성 시: 원문 Translation 행 **동기** 저장 → 나머지 5개 언어 `@Async` 번역
- 번역 실패 시: 해당 언어의 Translation row 없음 → 프론트는 원문 표시 + 번역 버튼 숨김
- `translationExecutor` 스레드풀 (core=4, max=8)

### 5-4. S3 이미지 업로드
- 게시글 저장 후 `@Async("s3Executor")` 호출
- MultipartFile은 요청 스코프 종료 문제로 **byte[]로 미리 읽어서** ImageData record로 전달
- URL 패턴: `https://{bucket}.s3.{region}.amazonaws.com/posts/{postId}/{uuid}`

### 5-5. 익명 게시글
- DB에는 항상 author_id 저장 (삭제/신고 처리 목적)
- `isAnonymous=true`면 응답 시 authorName/authorId를 null로 내려줌
- 본인 프로필에서는 익명 게시글 포함 조회 / 타인 프로필에서는 익명 게시글 제외

### 5-6. 채팅 (DM)
- `ChatRoom` 테이블 없음. `sender_id + receiver_id` 조합으로 대화 식별
- 시스템 메시지: `sender=null, isSystem=true, context_partner_id`로 대화 귀속 식별
- A-B 대화 조회 조건:
  ```
  (sender=A, receiver=B) OR (sender=B, receiver=A)
  OR (isSystem=true, receiver=A, contextPartner=B)
  OR (isSystem=true, receiver=B, contextPartner=A)
  ```
- 읽음 처리: 대화 조회 시 자동 일괄 처리 (별도 API 없음)

### 5-7. 멘토링 스케줄러
- `@Scheduled(cron = "0 0 0 1 3 *")` — 매년 3월 1일: 승격 + 1학기 매칭
- `@Scheduled(cron = "0 0 0 1 9 *")` — 매년 9월 1일: 2학기 매칭
- 승격 기준: `admissionYear < 현재연도 AND mentoringRole = MENTEE` → MENTOR
- 매칭 알고리즘: 1:1 순차 배정 → 남은 멘티는 배정 수 가장 적은 멘토에게 분산
- 매칭 완료 시 시스템 메시지 자동 삽입 (멘토/멘티 각각 1개씩)

### 5-8. commentCount 비정규화
- `Post.commentCount` 컬럼 존재 (성능 목적)
- 댓글 작성 시 `post.incrementCommentCount()`, 삭제 시 `decrementCommentCount()`
- 대댓글도 카운트에 포함

---

## 6. 구현된 API 목록

### Auth — `/api/auth`
| Method | Path | 설명 |
|--------|------|------|
| POST | /register | 회원가입 (@khu.ac.kr 이메일 인증 코드 발송) |
| POST | /verify-email | 이메일 인증 코드 확인 + JWT 발급 |
| POST | /profile | 최초 프로필 생성 (신입생=MENTEE 강제) |
| POST | /login | 로그인 (accessToken + refreshToken + hasProfile) |
| POST | /refresh | 리프레시 토큰으로 액세스 토큰 갱신 |
| POST | /logout | 로그아웃 (DB refresh token null 처리) |

### Member — `/api/members`
| Method | Path | 설명 |
|--------|------|------|
| GET | /me | 내 프로필 조회 |
| PUT | /me | 내 프로필 수정 (이름/학과/국적/입학년도/언어) |
| PATCH | /me/mentoring-role | 멘토링 역할 변경 |
| PATCH | /me/profile-image | 프로필 이미지 업로드 (multipart/form-data) |
| GET | /{memberId} | 타인 프로필 조회 |
| GET | /{memberId}/posts | 특정 멤버 게시글 목록 (본인=익명포함, 타인=익명제외) |

### Board — `/api/posts`
| Method | Path | 설명 |
|--------|------|------|
| POST | / | 게시글 작성 (multipart/form-data, 이미지 첨부 가능) |
| GET | /?boardType=FREE&language=KO | 게시판 목록 (페이징) |
| GET | /popular?language=KO | 인기 게시물 목록 (좋아요 10개 이상, 좋아요 순) |
| GET | /{postId}?language=KO | 게시글 상세 (isLiked, isOwner 포함) |
| DELETE | /{postId} | 게시글 삭제 (본인만) |
| POST | /{postId}/like | 좋아요 토글 (응답: true=추가, false=취소) |

### Comment — `/api/posts/{postId}/comments`
| Method | Path | 설명 |
|--------|------|------|
| POST | / | 댓글 작성 (parentId null=댓글, 값=대댓글) |
| GET | /?language=KO | 댓글 목록 (대댓글 children 포함) |

### Q&A — `/api/qnas`
| Method | Path | 설명 |
|--------|------|------|
| POST | / | 질문 작성 |
| GET | /?language=KO | 질문 목록 (페이징) |
| GET | /{qnaId}?language=KO | 질문 상세 + 답변 목록 |
| DELETE | /{qnaId} | 질문 삭제 (본인만) |
| POST | /{qnaId}/like | 질문 좋아요 토글 |
| POST | /{qnaId}/answers | 답변 작성 |
| DELETE | /{qnaId}/answers/{answerId} | 답변 삭제 (본인만) |
| POST | /{qnaId}/answers/{answerId}/adopt | 답변 채택 (질문 작성자만, 1회 고정) |
| POST | /{qnaId}/answers/{answerId}/like | 답변 좋아요 토글 |
| POST | /{qnaId}/comments | QnA 질문 댓글 작성 |
| GET | /{qnaId}/comments?language=KO | QnA 질문 댓글 목록 |
| POST | /{qnaId}/answers/{answerId}/comments | 답변 댓글 작성 |
| GET | /{qnaId}/answers/{answerId}/comments?language=KO | 답변 댓글 목록 |

### Comment (공통 삭제/좋아요) — `/api/comments`
| Method | Path | 설명 |
|--------|------|------|
| DELETE | /{commentId} | 댓글 삭제 (POST/QNA/ANSWER 공통, 본인만) |
| POST | /{commentId}/like | 댓글 좋아요 토글 |

### Mentoring — `/api/mentoring`
| Method | Path | 설명 |
|--------|------|------|
| GET | /me | 내 현재 학기 ACTIVE 매칭 정보 (상대방 프로필 포함) |

### Chat — `/api/chat`
| Method | Path | 설명 |
|--------|------|------|
| POST | / | 메시지 전송 (body: receiverId, content) |
| GET | / | 내 DM 목록 (대화 상대 + 마지막 메시지 + 안읽은 수) |
| GET | /{partnerId} | 특정 상대와 대화 내용 조회 + 읽음 처리 |

---

## 7. 미구현 / 다음 세션 작업 목록

**아래 항목 모두 구현 완료 (2026-04-10).**

### 7-1. Q&A / Answer 댓글 API ✅
- `CommentService.createComment()` → `targetType` 파라미터 추가 (POST/QNA/ANSWER 공통 처리)
- `QnaCommentController` 생성:
  - `POST /api/qnas/{qnaId}/comments`
  - `GET  /api/qnas/{qnaId}/comments`
  - `POST /api/qnas/{qnaId}/answers/{answerId}/comments`
  - `GET  /api/qnas/{qnaId}/answers/{answerId}/comments`
  - `DELETE /api/comments/{commentId}` (공통 삭제)
  - `POST   /api/comments/{commentId}/like` (공통 좋아요)

### 7-2. 프로필 이미지 업로드 API ✅
- `S3Service.uploadProfileImage(memberId, bytes, contentType)` 추가 (동기, URL 반환)
- `MemberService.updateProfileImage(memberId, bytes, contentType)` 추가
- `PATCH /api/members/me/profile-image` (multipart/form-data) 추가

### 7-3. 멘토링 매칭 조회 API ✅
- `MentorMenteeMatchRepository.findActiveMatchByMemberId()` @Query 추가
- `MentoringMatchResponse` DTO 생성
- `MentoringService.getMyMatch(memberId)` 추가
- `MentoringController` 생성: `GET /api/mentoring/me`

### 7-4. 인기 게시물 API ✅
- `PostRepository.findByLikeCountGreaterThanEqualOrderByLikeCountDesc()` 추가
- `PostService.getPopularPosts(language, pageable)` 추가
- `GET /api/posts/popular?language=KO` 추가

---

## 8. 보류 항목

| 항목 | 상태 | 비고 |
|------|------|------|
| 학사 가이드 백엔드 | 보류 | 팀 자료조사 미완료 — 완료 후 별도 git branch |
| 학사 퀴즈 | 보류 | 가이드 완성 후 연동 |
| Q&A 채택 마일리지 | 보류 | 채택 기능 구현 완료, 마일리지는 추후 |
| 게시글 신고/블라인드 | 보류 | Phase 2 |
| 푸시 알림 | 보류 | Phase 2 |
| 게시글 수정 API | 보류 | 삭제는 구현, 수정은 미구현 (PostTranslation 전체 재번역 필요) |
| 댓글 수정 API | 보류 | 삭제는 구현, 수정은 미구현 |

---

## 9. 환경변수 가이드

### 로컬 (현재 상태)
- DB 연결 정보: `application.yml`에 하드코딩 (GitHub 업로드 주의)
- JWT secret: `${JWT_SECRET:a2h1Z2xvYmFsaHViLXN1cGVyLXNlY3JldC1rZXktZm9yLWxvY2FsLWRldg==}` (기본값 있음)
- Azure/S3/Mail: 환경변수만 있음 (기본값 없음) → 테스트 시 환경변수 직접 설정 필요

### 운영 배포 시 필요한 환경변수
```
JWT_SECRET
AZURE_TRANSLATOR_KEY
AZURE_TRANSLATOR_ENDPOINT
AZURE_REGION
AWS_S3_BUCKET
AWS_S3_REGION
AWS_ACCESS_KEY
AWS_SECRET_KEY
MAIL_USERNAME
MAIL_PASSWORD
```

### GitHub 업로드 전 할 일
- [x] `application.yml` DB 정보 환경변수로 분리 완료
- [x] `application-local.yml` 생성 + `.gitignore` 추가 완료
- [x] `spring.profiles.active=local` 설정 완료

### 로컬 실행 시 주의
- `application-local.yml`은 `.gitignore`로 GitHub에 올라가지 않음
- 팀원이 새로 clone하면 `src/main/resources/application-local.yml`을 직접 만들어야 함
- 내용은 위 DB 정보(`localhost:5432`, `globalhub` / `globalhub1234`) 그대로

### 운영 배포 시 환경변수 설정 (EC2)
```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://{RDS_ENDPOINT}:5432/khu_global_hub
DB_USERNAME=...
DB_PASSWORD=...
DDL_AUTO=validate
JWT_SECRET=...
(나머지 Azure/S3/Mail 동일)
```

---

## 10. 프론트엔드 개발 가이드

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
실패 시 `success: false`, `data: null`, `message`에 에러 내용.

**언어 파라미터**: `?language=KO` (KO/EN/ZH/VI/ES/MN), 생략 시 기본 KO.

**페이지네이션**: `?page=0&size=20` (Spring Pageable 기본값).

---

### 인증 플로우

```
1. POST /api/auth/register        → 이메일 발송
2. POST /api/auth/verify-email    → accessToken + refreshToken + hasProfile 수신
3. hasProfile=false 이면 →
   POST /api/auth/profile         → 프로필 생성
4. 이후 모든 요청: Authorization: Bearer {accessToken}
5. 401 응답 시 → POST /api/auth/refresh 로 토큰 갱신
```

- `accessToken` 유효기간: 24시간
- `refreshToken` 유효기간: 7일 (DB 저장, 로그아웃 시 무효화)

---

### 주요 응답 필드 참고

**게시글 목록 (`PostSummaryResponse`)**:
`postId`, `boardType`, `title`, `authorName`(익명이면 null), `likeCount`, `commentCount`, `createdAt`, `isAnonymous`

**게시글 상세 (`PostDetailResponse`)**:
위 필드 + `content`, `imageUrls[]`, `isLiked`, `isOwner`

**댓글 (`CommentResponse`)**:
`commentId`, `content`, `authorName`(익명이면 null), `likeCount`, `isLiked`, `isOwner`, `createdAt`, `children[]`(대댓글 동일 구조)

**Q&A 목록 (`QnASummaryResponse`)**:
`qnaId`, `title`, `authorName`, `likeCount`, `answerCount`, `isAdopted`, `createdAt`

**멘토링 매칭 (`MentoringMatchResponse`)**:
`matchId`, `semester`, `status`, `myRole`(MENTOR/MENTEE), `matchedAt`, `partner`(ProfileResponse)

---

### 댓글 삭제/좋아요 통합 엔드포인트

게시글/Q&A/답변 댓글 모두 동일 경로 사용:
```
DELETE /api/comments/{commentId}
POST   /api/comments/{commentId}/like
```

---

### Swagger UI (로컬)
```
http://localhost:8080/swagger-ui/index.html
```
백엔드 실행 후 상세 요청/응답 스키마 확인 가능.

---

*최종 수정: 2026-04-10 | 백엔드 완료, 프론트엔드 개발 가이드 추가*

*최종 수정: 2026-04-10 | 미구현 4개 항목 완료 (댓글API/프로필이미지/멘토링조회/인기게시물)*
