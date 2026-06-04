# KHU Global Hub — 가이드 & 뱃지 시스템 개발 문서

> 최초 작성: 2026-05-17 / 최종 수정: 2026-06-02
> 작성자: 태 (tae)
> 브랜치: beta

---

## 개요

경희대 국제캠퍼스 신입 유학생을 위한 **가이드 + 카테고리 퀴즈 + 뱃지 시스템**입니다.

### 핵심 아이디어

> "가이드에서 퀴즈를 풀어 뱃지를 얻고, 그 뱃지가 커뮤니티에서 신뢰 지표가 된다"

```
가이드(학습) → 퀴즈(검증) → 뱃지(증명) → 커뮤니티(활용)
```

- **가이드**: 노션 자료 기반, 5개 카테고리 꿀팁 모음 (한/영 이중언어)
- **퀴즈**: 카테고리별 독립 퀴즈, 70% 이상 통과 시 뱃지 획득
- **뱃지**: 프로필 도감 + 커뮤니티 게시글/답변 옆 표시

**교수님 피드백 대응** (가이드 vs 커뮤니티 구분 모호 문제):
- 가이드 = 커뮤니티 신뢰도 시스템의 기반. 가이드 없이 뱃지 없고, 뱃지 없이 커뮤니티 신뢰도 없음.
- "수강신청 박사" 뱃지를 단 유저의 QnA 답변은 신뢰도가 다르다.

---

## 변경 이력 (2026-06-02)

| 항목 | 이전 | 변경 후 |
|------|------|---------|
| 퀴즈 탭 | 독립 탭 | 가이드 탭 내 카테고리별 버튼으로 통합 |
| 문제 수 | 14문제 (4개 카테고리 통합) | 23문제 (5개 카테고리 분리) |
| 재도전 제한 | 3회 제한 | 제한 없음 |
| 경희 온도 | 프로필에 `--°` 구조 | **완전 제거** → 뱃지로 대체 |
| 뱃지 시스템 | 없음 | **신규 추가** (DB + API + 프론트) |

---

## 뱃지 종류

| BadgeId | 뱃지 이름 | 획득 조건 |
|---------|----------|----------|
| `COURSE_REG` | 📚 수강신청 박사 | 수강신청 퀴즈 70% 이상 |
| `TRANSPORT` | 🚌 교통 박사 | 교통수단 퀴즈 70% 이상 |
| `FOOD` | 🍽️ 맛집 박사 | 맛집 퀴즈 70% 이상 |
| `CAMPUS_SITE` | 🔗 사이트 박사 | 학교 사이트 퀴즈 70% 이상 |
| `HUMANITIES` | 🎓 교양 박사 | 후마니타스 교양 퀴즈 70% 이상 |

---

## 퀴즈 문제 구성

총 **23문제**, 5개 카테고리, 전부 4지선다 객관식, 무제한 재도전

| 카테고리 | BadgeId | 문제 수 | 내용 |
|---------|---------|--------|------|
| 수강신청 | `COURSE_REG` | 6 | PC 신청, 네이비즘, F5 금지, 취소지연제, 학점세이브제 |
| 교통수단 | `TRANSPORT` | 5 | 교내 무료버스, 1550-1 방향, G5100, 영통역 도보, 지각 대처 |
| 맛집 | `FOOD` | 4 | 점심특선, 마라탕, 무한리필, 텐동 |
| 학교 사이트 | `CAMPUS_SITE` | 4 | 인포21, 스터디룸, 생협, 학사지원과 |
| 후마니타스 교양 | `HUMANITIES` | 4 | 필수이수학점, 선수과목, 배분이수, 국제캠 최대학점 |

---

## 파일 구조

### Backend (`KHU-Global-Hub-backend`)

```
src/main/java/com/khu/globalhub/campusguide/
├── domain/
│   ├── QuizQuestion.java         # 퀴즈 문제 엔티티
│   ├── QuizResult.java           # 퀴즈 결과 엔티티 (히스토리)
│   ├── BadgeId.java              # ★ NEW — 뱃지 종류 Enum (5개)
│   └── MemberBadge.java          # ★ NEW — 뱃지 획득 엔티티
├── infrastructure/
│   ├── QuizQuestionRepository.java
│   ├── QuizResultRepository.java
│   ├── QuizDataInitializer.java  # 서버 시작 시 문제 자동 시드
│   └── MemberBadgeRepository.java  # ★ NEW
├── application/
│   ├── QuizService.java
│   └── BadgeService.java         # ★ NEW — 뱃지 획득/조회 로직
└── presentation/
    ├── QuizController.java       # /api/quiz
    ├── BadgeController.java      # ★ NEW — /api/badges, /api/members/{id}/badges
    └── dto/
        ├── QuizQuestionResponse.java
        ├── QuizSubmitRequest.java
        ├── QuizSubmitResponse.java
        ├── MyQuizResultResponse.java
        └── BadgeResponse.java    # ★ NEW

src/main/resources/db/migration/
├── V1__baseline.sql
├── V2__delete_qna_answer_comments.sql
├── V3__drop_comment_target_type.sql
├── V4__drop_post_board_type.sql
└── V5__add_member_badges.sql     # ★ NEW — member_badges 테이블
```

### Frontend (`KHU-Global-Hub-frontend`)

```
src/
├── types/
│   ├── quiz.ts
│   └── badge.ts                  # ★ NEW — BadgeId, BadgeInfo, BADGE_META
├── api/
│   ├── quiz.ts
│   └── badge.ts                  # ★ NEW — badgeApi (earn, getMyBadges, getMemberBadges)
└── data/
    ├── khuGuide.ts               # ★ UPDATED — 노션 콘텐츠로 전면 교체 (5개 카테고리)
    └── quizQuestions.ts          # ★ UPDATED — 23문제, 카테고리별 분리, getQuestionsByCategory()

app/(main)/
├── guide.tsx                     # ★ UPDATED — 카테고리별 퀴즈 버튼 + 뱃지 획득 표시
├── quiz.tsx                      # ★ UPDATED — category URL 파라미터, 통과 시 뱃지 획득
└── profile.tsx                   # ★ UPDATED — 경희온도 제거, 뱃지 컬렉션 추가
```

---

## DB 스키마

```sql
-- V5__add_member_badges.sql
CREATE TABLE member_badges (
    id        BIGSERIAL PRIMARY KEY,
    member_id BIGINT      NOT NULL,
    badge_id  VARCHAR(50) NOT NULL,   -- BadgeId enum 값 (ex: 'COURSE_REG')
    earned_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_member_badge UNIQUE (member_id, badge_id)
);
```

- `UNIQUE (member_id, badge_id)` — 같은 뱃지를 중복 획득해도 무시됨
- `badge_id`는 `BadgeId` enum의 `.name()` 값 그대로 저장

---

## API 명세

### 퀴즈 API — `/api/quiz`

| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| GET | `/questions` | 불필요 | 전체 문제 목록 |
| GET | `/questions?category=수강신청` | 불필요 | 카테고리 필터 조회 |
| POST | `/submit` | 필요 | 답안 제출 → 채점 결과 반환 |
| GET | `/results/me` | 필요 | 내 퀴즈 히스토리 |
| GET | `/score/me` | 필요 | 내 최고 점수 |

### 뱃지 API — `/api/badges`

| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| POST | `/api/badges/{badgeId}` | 필요 | 뱃지 획득 (퀴즈 통과 시 호출) |
| GET | `/api/badges/me` | 필요 | 내 뱃지 목록 |
| **GET** | **`/api/members/{memberId}/badges`** | **불필요** | **특정 유저 뱃지 목록 (공개)** |

---

## 🔔 커뮤니티 개발자를 위한 뱃지 연동 가이드

> 커뮤니티(게시판/QnA) 화면에서 게시글 작성자 옆에 뱃지를 표시하려면 아래만 보세요.

### 1. 사용할 API

```
GET /api/members/{memberId}/badges
```

- **인증 불필요** (공개 API)
- `memberId`: 게시글/답변 작성자의 memberId

### 2. 응답 형식

```json
{
  "success": true,
  "message": "ok",
  "data": [
    {
      "badgeId": "COURSE_REG",
      "badgeNameKO": "수강신청 박사",
      "badgeNameEN": "Course Reg Expert",
      "emoji": "📚",
      "earnedAt": "2026-06-02T15:30:00"
    },
    {
      "badgeId": "TRANSPORT",
      "badgeNameKO": "교통 박사",
      "badgeNameEN": "Transport Expert",
      "emoji": "🚌",
      "earnedAt": "2026-06-02T16:00:00"
    }
  ]
}
```

- 뱃지가 없으면 `data: []` 빈 배열 반환

### 3. 프론트엔드 연동 예시 (TypeScript)

이미 만들어진 `badgeApi`를 그대로 쓰면 됩니다:

```typescript
import { badgeApi } from '@/src/api/badge';

// 게시글 작성자(memberId: 123)의 뱃지 가져오기
const badges = await badgeApi.getMemberBadges(123);
// → BadgeInfo[] 반환

// 대표 뱃지 1개 표시 (가장 최근 획득)
const latestBadge = badges[badges.length - 1];
if (latestBadge) {
  // latestBadge.emoji + latestBadge.badgeNameKO 표시
  console.log(`${latestBadge.emoji} ${latestBadge.badgeNameKO}`);
}
```

타입은 `src/types/badge.ts`에 이미 정의되어 있습니다:

```typescript
import type { BadgeInfo } from '@/src/types/badge';
```

### 4. 표시 권장 방식

게시글/답변 작성자 이름 옆에 **대표 뱃지 1개** (가장 최근 획득)를 이모지로 표시하는 것을 권장합니다.

```
[익명1] 📚  →  수강신청 관련 답변 신뢰도 ↑
[홍길동] 🎓  →  교양 관련 답변 신뢰도 ↑
```

뱃지 여러 개를 모두 표시하면 UI가 복잡해지므로 대표 1개를 권장합니다.

---

## 프론트엔드 화면 흐름

```
[가이드 탭]
  ├─ 카테고리 카드 (수강신청 / 교통 / 맛집 / 사이트 / 교양)
  │    ├─ 카드 탭 → 꿀팁 상세 보기
  │    ├─ 뱃지 미획득 → [퀴즈] 버튼 표시
  │    └─ 뱃지 획득 → 🏅 획득! 표시
  └─ 한/영 토글

[퀴즈 화면] (/(main)/quiz?category=COURSE_REG)
  ├─ 해당 카테고리 문제만 출제
  ├─ 4지선다, 무제한 재도전
  ├─ 70% 이상 → 뱃지 획득 API 자동 호출
  └─ 결과 화면에 뱃지 획득 메시지 표시

[프로필 탭]
  └─ 내 뱃지 컬렉션 (획득=컬러, 미획득=🔒 잠금)
```

---

## 백엔드 BC 규칙 준수 사항

- 뱃지 기능은 `campusguide` BC 내부에 완전히 격리되어 있습니다
- `member_id`는 Long 타입으로만 참조 (identity BC의 Member 엔티티 import 없음)
- 커뮤니티 팀원이 뱃지 데이터를 읽을 때는 REST API를 통해서만 접근 (리포지토리 직접 주입 금지)

---

## 추후 구현 예정

- [ ] 학과 카테고리 추가 (태경님이 학부 학과 정보 별도 제공 예정)
- [ ] 퀴즈 문제 관리자 추가/수정 API
- [ ] 뱃지 획득 시 푸시 알림

---

## 관련 파일

- [`docs/superpowers/specs/2026-06-02-guide-badge-system-design.md`](../superpowers/specs/2026-06-02-guide-badge-system-design.md) — 설계 스펙 문서 (프론트 레포)
- [`KHU_quiz.json`](./KHU_quiz.json) — 초기 퀴즈 문제 원본 (참고용, 현재는 quizQuestions.ts 기준)
