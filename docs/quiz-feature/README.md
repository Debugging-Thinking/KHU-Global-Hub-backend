# KHU Global Hub — 퀴즈 기능 개발 문서

> 최초 작성: 2026-05-17 / 최종 수정: 2026-05-18  
> 작성자: 태 (tae)  
> 브랜치: beta

---

## 개요

경희대 국제캠퍼스 신입생 대상 **꿀팁 퀴즈 기능**을 백엔드 + 프론트엔드에 구현했습니다.  
노션으로 정리된 학교 생활 가이드 자료를 바탕으로 **14개 객관식 문제**를 만들고,  
가이드(노션) → 퀴즈 → 결과 흐름으로 학습할 수 있습니다.

퀴즈 점수는 DB에 저장되며, 추후 **경희 온도** (당근마켓 온도 형태의 종합 점수)에 연동할 예정입니다.

---

## 퀴즈 문제 구성

총 **14문제**, 4개 카테고리, 전부 4지선다 객관식  
(초기 18문제에서 지엽적인 맛집 카테고리 4문제 제거)

| 카테고리 | 문제 수 | 내용 요약 |
|---|---|---|
| 수강신청 | 6 | PC 신청, 네이비즘, F5 금지, 취소지연제, 학점세이브제 등 |
| 교통수단 | 4 | 교내 무료 버스, 1550-1 방향, G5100 2층버스, 영통역 도보 |
| 후마니타스 교양 | 2 | 필수이수학점, 성찰과표현 선수과목 |
| 학교 사이트 | 2 | 인포21, 스터디룸 예약(libcal) |

원본 문제 데이터: [`KHU_quiz.json`](./KHU_quiz.json)  
(서버 최초 실행 시 `QuizDataInitializer`가 자동으로 DB에 시드합니다)

---

## 파일 구조

### Backend (`KHU-Global-Hub-backend`)

```
src/main/java/com/khu/globalhub/domain/quiz/
├── entity/
│   ├── QuizQuestion.java           # 퀴즈 문제 엔티티 (카테고리, 보기, 정답, 해설)
│   └── QuizResult.java             # 퀴즈 결과 엔티티 (멤버별 점수 기록)
├── dto/
│   ├── QuizQuestionResponse.java   # 문제 응답 DTO (정답 제외 — 클라이언트에 노출 안 함)
│   ├── QuizSubmitRequest.java      # 답안 제출 요청 DTO
│   ├── QuizSubmitResponse.java     # 채점 결과 응답 DTO
│   └── MyQuizResultResponse.java   # 내 결과 히스토리 DTO
├── repository/
│   ├── QuizQuestionRepository.java # findByCategory, existsByQuestion
│   └── QuizResultRepository.java   # 멤버별 결과 조회, 최고 점수 조회
├── service/
│   └── QuizService.java            # 문제 조회, 채점, 결과 저장 로직
├── controller/
│   └── QuizController.java         # REST API 엔드포인트
└── init/
    └── QuizDataInitializer.java    # 서버 시작 시 문제 자동 시드 (ApplicationRunner)
```

**수정된 기존 파일:**

| 파일 | 변경 내용 |
|---|---|
| `domain/member/entity/Profile.java` | `quizScore` 필드 추가, `updateQuizScore()` 메서드 추가 |
| `domain/member/dto/ProfileResponse.java` | `quizScore` 필드 추가 |
| `global/exception/ErrorCode.java` | `QUIZ_QUESTION_NOT_FOUND` 에러코드 추가 |

---

### Frontend (`KHU-Global-Hub-frontend`)

```
src/
├── types/
│   └── quiz.ts                 # 퀴즈 관련 타입 정의
├── api/
│   └── quiz.ts                 # 퀴즈 API 호출 함수
└── data/
    ├── khuGuide.ts             # 가이드 콘텐츠 데이터 (카테고리별 꿀팁)
    └── quizQuestions.ts        # 로컬 퀴즈 데이터 + gradeLocally() 함수
                                #   - 백엔드 미연결 시 fallback 데이터로 사용
                                #   - 정답 포함 (결과 화면 해설 표시용)

app/(main)/
└── quiz.tsx                    # 퀴즈 화면 (홈/퀴즈/결과 흐름)
```

**수정된 기존 파일:**

| 파일 | 변경 내용 |
|---|---|
| `app/(main)/_layout.tsx` | 탭바에 '퀴즈' 탭 추가 (`school-outline` 아이콘) |
| `app/(main)/profile.tsx` | '경희 온도' 카드 추가 (현재 `--°` 표시, 추후 점수 반영 예정) |
| `app/(auth)/login.tsx` | 웹 레이아웃 버그 수정 (`keyboardAvoiding` → `scrollable`) |

---

## API 명세

**Base URL:** `http://13.125.205.177:8080/api/quiz`  
**인증:** JWT Bearer Token 필요 (submit, results/me, score/me)

| Method | Endpoint | 설명 |
|---|---|---|
| GET | `/questions` | 전체 문제 목록 조회 |
| GET | `/questions?category=수강신청` | 카테고리 필터 조회 |
| POST | `/submit` | 답안 제출 → 채점 결과 반환 |
| GET | `/results/me` | 내 퀴즈 히스토리 조회 (도전 횟수 확인에도 사용) |
| GET | `/score/me` | 내 최고 점수 조회 |

**POST /submit 요청 예시:**
```json
{
  "answers": [
    { "questionId": 1, "selectedOption": 1 },
    { "questionId": 2, "selectedOption": 2 }
  ]
}
```

**POST /submit 응답 예시:**
```json
{
  "correctCount": 1,
  "totalCount": 2,
  "score": 50.0,
  "results": [
    {
      "questionId": 1,
      "correct": true,
      "correctAnswer": 1,
      "explanation": "신입생은 사전 희망과목 담기..."
    }
  ]
}
```

---

## 데이터 시드 방식

`QuizDataInitializer.java` (ApplicationRunner)가 서버 시작 시 아래 조건으로 실행됩니다:

```
if (quizQuestionRepository.count() == 0) → DB에 14개 문제 자동 삽입
```

문제가 이미 있으면 실행하지 않으므로 중복 삽입 없음.

---

## 프론트엔드 화면 흐름

```
퀴즈 탭 진입
    ↓
[홈 화면]
  ├─ 가이드 읽기 → 노션 페이지 새 탭으로 열기
  │    (https://www.notion.so/KHU-GUIDE-33e9ce2546d28061af04cae28b742b21)
  └─ 퀴즈 시작하기 (남은 도전 횟수 표시)
         ↓ 도전 횟수 소진 시 잠금 UI 표시
[퀴즈 화면]
  - 문제당 답 자유롭게 변경 가능 (다음 문제 넘기기 전까지)
  - 중간 정답/해설 표시 없음 (점수 조작 방지)
  - 애니메이션 프로그레스 바
         ↓
[결과 화면]
  - 최종 점수 + 문제별 정오 + 해설 전체 표시
  - 홈으로 돌아가기만 가능 (재시작 불가)
```

---

## 도전 횟수 제한 (3회)

점수 조작 방지를 위해 퀴즈 도전은 **최대 3회**로 제한됩니다.

| 우선순위 | 방식 | 설명 |
|---|---|---|
| 1순위 | 백엔드 API | `GET /results/me` 결과 수로 실제 도전 횟수 확인 |
| 2순위 | AsyncStorage | 백엔드 미연결 시 로컬 저장값 사용 (`khu_quiz_attempts`) |

- 퀴즈 완료 시 AsyncStorage 카운트 즉시 +1 (오프라인 환경 대비)
- 3회 소진 후 홈 화면에 잠금 UI 표시, 퀴즈 시작 불가
- **최고 점수**가 프로필 경희 온도에 반영 예정

---

## 추후 구현 예정

- [ ] **경희 온도** 점수 계산 로직 구현
  - 퀴즈 최고점수 + 멘토링 참여도 + 커뮤니티 활동 등 종합 반영
  - 현재 프로필에 `--°` 형태로 자리만 만들어 둔 상태
- [ ] 퀴즈 문제 추가 (학과별 정보, 장학금, 교내 시설 등 실용적인 주제)
- [ ] 퀴즈 문제 관리자 추가/수정 API
- [ ] 카테고리별 랭킹 기능

---

## 관련 파일

- [`KHU_quiz.json`](./KHU_quiz.json) — 14개 퀴즈 문제 원본 데이터 (JSON)
