# ARCHITECTURE.md — KHU Global Hub 백엔드 아키텍처

> 팀 개발 참고용 설계 문서. 새 기능 확장 전 이 문서를 먼저 읽어주세요.
> 민감 정보(실제 IP, 키, 엔드포인트)는 포함하지 않습니다 — 운영 접속 정보는 팀 내부 채널에서 관리.
> **코드 변경 시 이 문서도 함께 업데이트할 것.**
>
> 🛠 로컬 실행 방법은 [`README-local.md`](./README-local.md) 참고.

---

## 1. 프로젝트 현황 (2026-05-22)

- **백엔드**: Spring Boot 3.4.5 / Java 21 / PostgreSQL — AWS EC2 운영 배포 중
- **프론트엔드**: React Native (Expo) + TypeScript — 핵심 기능 구현 완료
- **웹**: Expo Web 빌드로 동일 코드를 웹에서도 사용 — AWS EC2 Nginx로 배포 중
- **배포 URL**: `http://{EC2_IP}` (포트 80, Nginx static serving)

---

## 2. 도메인 설계 핵심 결정사항

### 2-1. Member / Profile 분리
- `members`: 인증 전용 (email, password, refreshToken, isEmailVerified)
- `profiles`: 프로필 데이터 (name, department, nationality, admissionYear, language, mentoringRole)
- Profile row 존재 여부 = 프로필 완성 여부
- 로그인 응답에 `hasProfile` 플래그 포함 → 프론트가 프로필 생성 화면으로 라우팅

### 2-2. JWT 구조
- AccessToken: 24시간, stateless (DB 미저장)
- RefreshToken: 7일, `members.refresh_token`에 저장
- SecurityContext principal = `Long memberId`
- `SecurityUtil.getCurrentMemberId()`로 현재 유저 조회

### 2-3. 번역 비동기 처리
- 작성 시: 원문 Translation 행 **동기** 저장 → 나머지 5개 언어 `@Async` 번역
- 번역 실패 → 해당 Translation 행 없음 → 조회 시 EN 폴백 → 그것도 없으면 첫 번째 번역본
- `translationExecutor`: core=4, max=8 스레드풀

### 2-4. S3 이미지 업로드
- 게시글 이미지: 저장 후 `@Async("s3Executor")` 업로드
- MultipartFile을 요청 스코프 종료 전 `byte[]`로 먼저 읽어서 `ImageData` record로 전달
- 게시글 URL 패턴: `posts/{postId}/{uuid}`
- 프로필 이미지: 동기 처리, `profiles/{memberId}/{uuid}`

### 2-5. 익명 게시글 & 익명 번호 시스템
- DB에 항상 `author_id` 저장 (삭제/신고 처리 목적)
- `isAnonymous=true`일 때 `authorName`은 "익명N" (N=할당 번호) 형식으로 반환
- `AnonymousAlias` 엔티티: `(contextType, contextId, memberId)` 유니크 조합으로 번호 할당
  - `AliasContextType.POST` — 게시글 1개 = 1 컨텍스트
  - `AliasContextType.QNA` — Q&A 1개 + 해당 답변들 전체 = 1 컨텍스트 공유
- 게시글 작성자는 익명1, 이후 익명 댓글 작성자는 순번 할당 (익명2, 3, ...)
- 같은 사람이 같은 컨텍스트에서 여러 번 익명 글을 써도 동일 번호 유지
- **각 게시글/Q&A는 독립**: 다른 게시글에서 할당받은 익명 번호와 무관

### 2-6. 채팅 (DM)
- `ChatRoom` 테이블 없음. `sender_id + receiver_id` 조합으로 대화 식별
- 시스템 메시지: `sender=null, isSystem=true, contextPartner_id`로 대화 귀속 식별
- 읽음 처리: 대화 조회 시 자동 일괄 처리 (별도 API 없음)
- 현재 방식: 폴링(polling) — 프론트에서 주기적으로 GET 호출

### 2-7. 멘토링 스케줄러
- `@Scheduled(cron = "0 0 0 1 3 *")` — 매년 3월 1일 자정 (UTC 기준)
- `@Scheduled(cron = "0 0 0 1 9 *")` — 매년 9월 1일 자정 (UTC 기준)
- 매칭 알고리즘: max(멘토수, 멘티수)번 순환 배정

### 2-8. commentCount 비정규화
- `Post.commentCount` 컬럼 존재 (성능 목적)
- 댓글/대댓글 작성 시 `post.incrementCommentCount()`, 삭제 시 `decrementCommentCount()`

### 2-9. 댓글 통합 설계
- Comment 엔티티가 POST/QNA/ANSWER 모두 처리
- `targetType` (CommentTargetType enum) + `targetId`로 대상 식별
- 삭제·좋아요 공통 엔드포인트: `DELETE /api/comments/{id}`, `POST /api/comments/{id}/like`

### 2-10. Q&A 비즈니스 규칙
- 채택 완료된 질문에는 답변 추가 불가 (`QNA_ALREADY_ADOPTED`)
- 본인 질문에 본인이 답변 불가 (`SELF_ANSWER_NOT_ALLOWED`)
- 1인 1답변 제한 (`ANSWER_ALREADY_EXISTS`)
- 답변 채택 시: `Answer.isAdopted = true`, `QnA.isAdopted = true` (1회만 가능)

### 2-11. 비밀번호 재설정
- `POST /api/auth/forgot-password`: 이메일 인증된 계정만, 재설정 코드 발송 (10분 유효)
- `POST /api/auth/reset-password`: 코드 검증 후 새 비번 적용
- 재설정 성공 시 refreshToken 무효화 → 모든 기기 강제 로그아웃

---

## 3. 운영 배포 정보

### 인프라 구성
- EC2 (ap-northeast-2, t3.micro) — 백엔드(8080) + Nginx 웹(80)
- RDS PostgreSQL 17 (ap-northeast-2, db.t3.micro)
- S3 (ap-northeast-2) — 이미지 저장
- Azure Translator Free F0 티어

> **실제 IP, SSH 키, RDS 엔드포인트 등 접속 정보는 팀 내부 채널에서 관리.**

### 백엔드 재배포 절차
```bash
# 로컬에서 빌드 후 EC2 전송
./gradlew bootJar
scp -i "{KEY}.pem" build/libs/globalhub-0.0.1-SNAPSHOT.jar ubuntu@{EC2_IP}:~/app.jar

# EC2에서 재시작 (deploy.sh 사용)
ssh -i "{KEY}.pem" ubuntu@{EC2_IP} "nohup bash ~/deploy.sh > /tmp/deploy.log 2>&1 &"

# deploy.sh 내용 (EC2 ~/deploy.sh)
# pkill -f app.jar; sleep 2; source ~/.env; nohup java -jar ~/app.jar > ~/app.log 2>&1 &
```

### 프론트엔드 웹 재배포 절차
```bash
# 1. 프론트 빌드
cd frontend
npx expo export --platform web   # dist/ 폴더 생성

# 2. EC2 업로드 및 Nginx 반영
scp -i "{KEY}.pem" -r dist/* ubuntu@{EC2_IP}:~/web/
ssh -i "{KEY}.pem" ubuntu@{EC2_IP} "sudo cp -r ~/web/* /var/www/html/globalhub/"
```

### Nginx 설정 (EC2 /etc/nginx/sites-available/globalhub)
```nginx
server {
    listen 80;
    server_name _;
    root /var/www/html/globalhub;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8080;   # 끝슬래시 X — /api/ prefix 유지해서 백엔드로 전달
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### EC2 환경변수 파일 (~/.env)
```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://{RDS_ENDPOINT}:5432/khu_global_hub
DB_USERNAME=...
DB_PASSWORD=...
DDL_AUTO=validate
JWT_SECRET=...
AZURE_TRANSLATOR_KEY=...
AZURE_REGION=...
AWS_S3_BUCKET=...
AWS_S3_REGION=...
AWS_ACCESS_KEY=...
AWS_SECRET_KEY=...
MAIL_USERNAME=...
MAIL_PASSWORD=...
```

### 스키마 변경 시
- `DDL_AUTO=validate` 이므로 테이블 변경 시 SQL 직접 실행 필요
- 새 테이블 추가 시 `DDL_AUTO=update` 로 임시 변경 후 배포, 확인 후 다시 `validate`로 복구

---

## 4. API 전체 목록

### Auth — `/api/auth`
| Method | Path | 설명 |
|--------|------|------|
| POST | /register | 이메일 인증 코드 발송 (@khu.ac.kr 전용) |
| POST | /verify-email | 코드 확인 + JWT 발급 (hasProfile 포함) |
| POST | /profile | 최초 프로필 생성 (신입생=MENTEE 강제) |
| POST | /login | 로그인 |
| POST | /refresh | 액세스 토큰 갱신 |
| POST | /logout | DB refresh token null 처리 |
| POST | /forgot-password | 비밀번호 재설정 코드 발송 (이메일 인증된 계정만, 10분 유효) |
| POST | /reset-password | 코드 검증 후 새 비번 적용 (성공 시 모든 기기 강제 로그아웃) |

### Member — `/api/members`
| Method | Path | 설명 |
|--------|------|------|
| GET | /me | 내 프로필 조회 |
| PUT | /me | 프로필 수정 |
| PATCH | /me/mentoring-role | 멘토링 역할 변경 |
| PATCH | /me/profile-image | 프로필 이미지 업로드 (multipart) |
| GET | /{memberId} | 타인 프로필 조회 |
| GET | /{memberId}/posts | 특정 멤버 게시글 목록 |

### Board — `/api/posts`
| Method | Path | 설명 |
|--------|------|------|
| POST | / | 게시글 작성 (multipart, 이미지 가능) |
| GET | /?boardType=FREE&language=KO | 게시판 목록 (페이징) |
| GET | /popular?language=KO | 인기 게시물 (좋아요 10개 이상) |
| GET | /{postId}?language=KO | 게시글 상세 |
| DELETE | /{postId} | 게시글 삭제 (작성자만, 댓글+좋아요 cascade) |
| POST | /{postId}/like | 좋아요 토글 |

### Comment
| Method | Path | 설명 |
|--------|------|------|
| POST | /api/posts/{postId}/comments | 게시글 댓글 작성 |
| GET | /api/posts/{postId}/comments?language=KO | 게시글 댓글 목록 |
| POST | /api/qnas/{qnaId}/comments | QnA 댓글 작성 |
| GET | /api/qnas/{qnaId}/comments?language=KO | QnA 댓글 목록 |
| POST | /api/qnas/{qnaId}/answers/{answerId}/comments | 답변 댓글 작성 |
| GET | /api/qnas/{qnaId}/answers/{answerId}/comments?language=KO | 답변 댓글 목록 |
| DELETE | /api/comments/{commentId} | 댓글 삭제 (공통, 대댓글+좋아요 cascade) |
| POST | /api/comments/{commentId}/like | 댓글 좋아요 토글 (공통) |

### Q&A — `/api/qnas`
| Method | Path | 설명 |
|--------|------|------|
| POST | / | 질문 작성 |
| GET | /?language=KO | 질문 목록 |
| GET | /{qnaId}?language=KO | 질문 상세 + 답변 |
| DELETE | /{qnaId} | 질문 삭제 (답변+좋아요 cascade) |
| POST | /{qnaId}/like | 좋아요 토글 |
| POST | /{qnaId}/answers | 답변 작성 (채택 후 불가, 본인 질문 불가, 1인 1답) |
| DELETE | /{qnaId}/answers/{answerId} | 답변 삭제 |
| POST | /{qnaId}/answers/{answerId}/adopt | 답변 채택 (질문 작성자만, 1회) |
| POST | /{qnaId}/answers/{answerId}/like | 답변 좋아요 토글 |

### Mentoring — `/api/mentoring`
| Method | Path | 설명 |
|--------|------|------|
| GET | /me | 내 현재 ACTIVE 매칭 정보 (상대 프로필 포함) |

### Chat — `/api/chat`
| Method | Path | 설명 |
|--------|------|------|
| POST | / | 메시지 전송 |
| GET | / | DM 목록 (대화 상대 + 마지막 메시지 + 안읽은 수) |
| GET | /{partnerId} | 특정 상대와 대화 내용 + 읽음 처리 |

---

## 5. 공통 규칙 (프론트 연동 시 필수)

### 인증 헤더
```
Authorization: Bearer {accessToken}
```

### 공통 응답
```json
{ "success": true, "message": "ok", "data": { ... } }
```
실패 시: `success: false`, `data: null`, `message`에 에러 내용.

### 언어 파라미터
`?language=KO` (KO / EN / ZH / VI / ES / MN), 생략 시 KO.

### 페이지네이션
`?page=0&size=20` (Spring Pageable 기본값).

### 인증 플로우
```
1. POST /api/auth/register        → 이메일 발송
2. POST /api/auth/verify-email    → accessToken + refreshToken + hasProfile
3. hasProfile=false → POST /api/auth/profile
4. 이후 모든 요청: Authorization: Bearer {accessToken}
5. 401 응답 → POST /api/auth/refresh
```

---

## 6. 프론트엔드 구조

### 기술 스택
- Expo SDK 52 + React Native + TypeScript
- expo-router (파일 기반 라우팅)
- Zustand (`authStore` — 토큰, 프로필 상태 관리)
- axios 인터셉터 — 401 시 자동 토큰 갱신 후 재요청

### 주요 파일 구조
```
frontend/
├── app/
│   ├── (auth)/          # 로그인, 회원가입, 이메일인증, 프로필설정
│   └── (main)/          # 탭 기반 메인 화면
│       ├── _layout.tsx  # 탭바 설정 (숨김 라우트 포함)
│       ├── index.tsx    # 게시판 목록
│       ├── board/[postId].tsx    # 게시글 상세 + 댓글
│       ├── board/create.tsx      # 게시글 작성
│       ├── qna/index.tsx         # Q&A 목록
│       ├── qna/[qnaId].tsx       # Q&A 상세 + 답변
│       ├── qna/create.tsx        # Q&A 작성
│       ├── chat/                 # 채팅 목록 + 상세
│       ├── mentoring.tsx         # 멘토링 매칭 정보
│       └── profile.tsx           # 내 프로필
├── src/
│   ├── api/             # boardApi, qnaApi, authApi, ...
│   ├── store/           # authStore (Zustand)
│   ├── types/           # board.ts, qna.ts, auth.ts, ...
│   └── components/      # Screen, 공용 컴포넌트
└── constants/
    └── theme.ts         # Colors, Typography, Spacing, Radius, Shadow
```

### 탭바 숨김 라우트
`_layout.tsx`에서 `href: null`로 숨긴 라우트들:
- `board`, `board/[postId]`, `board/create`
- `qna/[qnaId]`, `qna/create`
- `chat/[partnerId]`

### 플랫폼 분기 주의사항
- `Alert.alert`은 웹에서 동작 안 함 → `confirmAction` 헬퍼 사용
  ```ts
  function confirmAction(title, message, onConfirm) {
    if (Platform.OS === 'web') {
      if (window.confirm(`${title}\n${message}`)) onConfirm();
    } else {
      Alert.alert(title, message, [...]);
    }
  }
  ```

---

## 7. 보류 항목

| 항목 | 비고 |
|------|------|
| 학사 가이드 백엔드 | 팀 자료조사 완료 후 별도 브랜치 |
| 학사 퀴즈 | 가이드 완성 후 연동 |
| Q&A 채택 마일리지 | 채택 기능 구현 완료, 마일리지는 추후 |
| 게시글 신고/블라인드 | Phase 2 |
| 푸시 알림 | Phase 2 |
| 게시글·댓글 수정 API | 삭제 구현 완료, 수정은 미구현 |
| Q&A·답변 댓글 UI | 백엔드 API 있음, 프론트 미구현 |
| 이메일 인증 코드 재발송 | 미구현 |

---

## 8. 알려진 기술 부채

| 항목 | 설명 |
|------|------|
| Scheduler 시간대 | cron이 서버 JVM 시간 기준. EC2 UTC 설정 시 KST와 9시간 차이 |
| S3 고아 파일 | 게시글 삭제 시 S3 이미지 삭제 안 됨 |
| 프로필 이미지 교체 | 이미지 업데이트 시 기존 S3 파일 삭제 안 됨 |
| commentCount 동시성 | 수동 증감 방식 — 대규모 동시 요청 시 race condition 가능 |
| 채팅 폴링 | 실시간성 낮음. 사용자 증가 시 WebSocket 전환 권장 |
| APK 빌드 | 최신 프론트 코드로 EAS Build 재실행 필요 (현재 배포된 APK는 이전 버전) |

---

## 9. 개발 환경 주의사항

- 로컬 실행 방법은 [`README-local.md`](./README-local.md) 참고
- `application-local.yml`은 `.gitignore` 처리. 새 팀원은 `application-local.yml.example`을 복사해서 생성
- `application-prod.yml`도 `.gitignore` 처리. 운영 설정은 EC2 환경변수로 관리
- `*.pem`, `*.env` 파일은 `.gitignore` 처리. SSH 키·비밀값 절대 커밋 금지
- 메일/Azure/AWS 키는 `application.yml`에 더미 기본값이 있어 **키 없이도 앱 실행 가능**. 단 해당 기능(이메일 발송/번역/이미지 업로드) 실제 테스트 시에는 환경변수로 실제 키 주입 필요
- `DDL_AUTO=create`는 로컬 전용. 운영(`validate`)에서 스키마 변경 시 SQL 직접 실행 필요
