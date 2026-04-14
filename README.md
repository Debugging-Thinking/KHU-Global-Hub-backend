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
| DB | PostgreSQL 16 (로컬: Docker) |
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

- **다국어 게시판** — 게시글·댓글 작성 시 6개 언어(한·영·중·베트남·스페인·몽골)로 자동 번역 (Azure Translator)
- **익명 번호 시스템** — 게시글/Q&A별 독립 익명 컨텍스트, 작성자=익명1 → 이후 댓글 익명2, 3, ...
- **Q&A 채택 시스템** — 질문에 답변을 달고 질문자가 채택, 채택 후 추가 답변 불가
- **멘토-멘티 자동 매칭** — 매년 3월/9월 1일 스케줄러로 자동 매칭, 시스템 메시지 발송
- **1:1 DM 채팅** — 멘토-멘티 및 자유 메시지, 읽음 처리 포함
- **이미지 업로드** — 게시글·프로필 사진 AWS S3 비동기 업로드
- **이메일 인증** — @khu.ac.kr 이메일 인증 기반 가입

---

## 4. 로컬 개발 환경

### 백엔드 사전 준비
- Java 21 (JDK)
- Docker Desktop

### 1. application-local.yml 생성

`src/main/resources/application-local.yml`은 `.gitignore` 처리되어 있으므로 직접 생성:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/khu_global_hub
    username: globalhub
    password: globalhub1234
  jpa:
    hibernate:
      ddl-auto: create   # 최초 실행: create / 이후: update
    show-sql: true
```

### 2. DB 실행

```bash
docker compose up -d
```

### 3. 백엔드 실행

```bash
./gradlew bootRun
```

### 4. Swagger UI (로컬)

```
http://localhost:8080/swagger-ui/index.html
```

### 5. 프론트엔드 실행

```bash
cd frontend
npm install
npx expo start
```

브라우저 웹: `w` 키 / Android 에뮬레이터: `a` 키

### 6. 프론트엔드 웹 빌드 & 배포

```bash
cd frontend
npx expo export --platform web   # dist/ 생성

scp -i "{KEY}.pem" -r dist/* ubuntu@{EC2_IP}:~/web/
ssh -i "{KEY}.pem" ubuntu@{EC2_IP} "sudo cp -r ~/web/* /var/www/html/globalhub/"
```

> Azure Translator, AWS S3, Gmail SMTP 기능은 해당 환경변수 없이는 동작하지 않습니다.

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
├── global/
│   ├── common/       ApiResponse, BaseTimeEntity
│   ├── config/       AsyncConfig, SecurityConfig, S3Config, JpaConfig
│   ├── enums/        Language, BoardType, CommentTargetType, AliasContextType, ...
│   ├── exception/    ErrorCode, CustomException, GlobalExceptionHandler
│   ├── infra/        TranslationService, S3Service
│   ├── jwt/          JwtTokenProvider, JwtAuthenticationFilter
│   └── util/         SecurityUtil
└── domain/
    ├── anonymous/    AnonymousAlias 엔티티 + 익명 번호 서비스
    ├── auth/         회원가입, 이메일 인증, 로그인
    ├── member/       프로필 관리, 멘토링 역할
    ├── board/        게시판 (FRESHMAN/FREE/GRADUATE)
    ├── comment/      댓글·대댓글 (POST/QNA/ANSWER 통합)
    ├── qna/          Q&A + 답변 채택
    ├── mentoring/    멘토-멘티 매칭 + 스케줄러
    └── chat/         1:1 DM
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
| POST | `/` | 게시글 작성 (multipart/form-data, 이미지 가능) |
| GET | `/?boardType=FREE&language=KO` | 게시판 목록 (페이징) |
| GET | `/popular?language=KO` | 인기 게시물 (좋아요 10개 이상) |
| GET | `/{postId}?language=KO` | 게시글 상세 (isLiked, isOwner 포함) |
| DELETE | `/{postId}` | 게시글 삭제 (작성자만) |
| POST | `/{postId}/like` | 좋아요 토글 |

### Comment

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/posts/{postId}/comments` | 게시글 댓글 작성 (parentId 있으면 대댓글) |
| GET | `/api/posts/{postId}/comments?language=KO` | 댓글 목록 (대댓글 포함) |
| POST | `/api/qnas/{qnaId}/comments` | QnA 댓글 작성 |
| GET | `/api/qnas/{qnaId}/comments?language=KO` | QnA 댓글 목록 |
| POST | `/api/qnas/{qnaId}/answers/{answerId}/comments` | 답변 댓글 작성 |
| GET | `/api/qnas/{qnaId}/answers/{answerId}/comments?language=KO` | 답변 댓글 목록 |
| DELETE | `/api/comments/{commentId}` | 댓글 삭제 (공통) |
| POST | `/api/comments/{commentId}/like` | 댓글 좋아요 토글 (공통) |

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

### Chat — `/api/chat`

| Method | Path | 설명 |
|--------|------|------|
| POST | `/` | 메시지 전송 |
| GET | `/` | DM 목록 (대화 상대 + 마지막 메시지 + 안읽은 수) |
| GET | `/{partnerId}` | 특정 상대와 대화 내용 조회 + 읽음 처리 |

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

**언어 파라미터**: `?language=KO` (KO/EN/ZH/VI/ES/MN), 생략 시 기본 KO.

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
| 게시글·댓글 수정 API | 삭제 구현 완료, 수정은 미구현 |
| Q&A·답변 댓글 UI | 백엔드 API 있음, 프론트 미구현 |
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

*경희대학교 디자인씽킹 팀프로젝트 — 2026*
