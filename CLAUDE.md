# KHU Global Hub — 프로젝트 고정 정보

## 레포지토리
- 백엔드: https://github.com/Debugging-Thinking/KHU-Global-Hub-backend
- 프론트엔드: https://github.com/Debugging-Thinking/KHU-Global-Hub-frontend
- 작업 브랜치: **beta** (main은 원본 유지)

## 서버
- EC2 주소: 13.125.205.177
- 백엔드 포트: 8080
- DB: PostgreSQL, 포트 5432 (외부 접근 차단됨 — SSH 터널 필요)
- PEM 키: 이 컴퓨터에 없음, 서버 권한 있는 팀원에게 요청 필요

## 로컬 경로
- 백엔드: C:\design_thinking\KHU-Global-Hub-backend
- 프론트엔드: C:\design_thinking\KHU-Global-Hub-frontend

## 기술 스택
- 백엔드: Spring Boot, JPA, PostgreSQL, JWT
- 프론트엔드: React Native (Expo Router), TypeScript

## 구현 완료 기능

### 가이드 & 뱃지 시스템 (campusguide BC — 태경 담당)
- **가이드**: 5개 카테고리 꿀팁 (수강신청/교통/맛집/학교사이트/후마니타스교양), 한/영 이중언어
- **카테고리별 퀴즈**: 23문제, 4지선다, 70% 이상 통과 시 뱃지 획득, 무제한 재도전
- **뱃지 시스템**: 5종 뱃지 (COURSE_REG / TRANSPORT / FOOD / CAMPUS_SITE / HUMANITIES)
  - DB: `member_badges` 테이블 (V5 마이그레이션)
  - 백엔드: `campusguide/domain/BadgeId.java`, `MemberBadge.java`, `BadgeService.java`, `BadgeController.java`
  - 문서: `docs/quiz-feature/README.md`
- **경희 온도 제거됨** — 뱃지 컬렉션으로 대체

### 🔔 커뮤니티 개발자 (게시판/QnA 담당)에게
게시글·답변 작성자 옆에 뱃지를 표시하려면 아래 API 하나만 쓰면 됩니다:

```
GET /api/members/{memberId}/badges   ← 인증 불필요, 공개 API
```

응답:
```json
{
  "data": [
    { "badgeId": "COURSE_REG", "badgeNameKO": "수강신청 박사", "emoji": "📚", "earnedAt": "..." },
    { "badgeId": "FOOD",       "badgeNameKO": "맛집 박사",     "emoji": "🍽️", "earnedAt": "..." }
  ]
}
```

프론트엔드에서는 `src/api/badge.ts`의 `badgeApi.getMemberBadges(memberId)` 함수를 바로 쓰면 됩니다.
상세 연동 가이드: `docs/quiz-feature/README.md` → "커뮤니티 개발자를 위한 뱃지 연동 가이드" 섹션

## 추후 구현 예정
- 학과 카테고리 추가 (학부 학과 정보, 태경님이 별도 제공 예정)
- 퀴즈 문제 관리자 추가/수정 API
