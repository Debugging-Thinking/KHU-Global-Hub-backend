-- 강의평 선택 지표: 수업방식 / 발표·조모임·과제·한국어 사용 빈도 (모두 nullable, 리뷰별 응답 → 집계).
ALTER TABLE course_reviews ADD COLUMN attendance_type   VARCHAR(20);  -- OFFLINE / ONLINE / BLENDED
ALTER TABLE course_reviews ADD COLUMN presentation_freq VARCHAR(20);  -- LOW / MEDIUM / HIGH
ALTER TABLE course_reviews ADD COLUMN group_work_freq   VARCHAR(20);
ALTER TABLE course_reviews ADD COLUMN assignment_freq   VARCHAR(20);
ALTER TABLE course_reviews ADD COLUMN korean_usage      VARCHAR(20);
