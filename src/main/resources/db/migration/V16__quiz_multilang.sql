-- 퀴즈 다국어 전환 — 게시판/강의평과 동일한 사전번역 방식.
-- 텍스트(question/options/explanation)를 quiz_question_translations로 분리하고,
-- quiz_questions는 메타(category/answer_index)만 남긴다.

-- 1) 번역 테이블 생성 (문항 1개당 언어별 1행, 최대 6).
--    options는 보기 리스트를 JSON 배열 문자열로 직렬화해 한 컬럼에 저장.
CREATE TABLE quiz_question_translations (
    id          BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    language    VARCHAR(20) NOT NULL,
    question    TEXT NOT NULL,
    options     TEXT NOT NULL,          -- JSON 배열 (예: ["보기1","보기2","보기3"])
    explanation TEXT,
    CONSTRAINT uq_quiz_question_translation UNIQUE (question_id, language)
);

CREATE INDEX idx_quiz_question_translations_question ON quiz_question_translations(question_id);

-- 2) 기존 문항을 KO 원문 번역 행으로 이관 (운영 18문항 보존).
--    quiz_options(option_index 순)를 JSON 배열로 집계 → options 컬럼에 직렬화 저장.
INSERT INTO quiz_question_translations (question_id, language, question, options, explanation)
SELECT q.id,
       'KO',
       q.question,
       COALESCE(
           (SELECT json_agg(o.option_text ORDER BY o.option_index)::text
            FROM quiz_options o
            WHERE o.question_id = q.id),
           '[]'
       ),
       q.explanation
FROM quiz_questions q;

-- 3) 이관 완료 → 기존 옵션 테이블/텍스트 컬럼 제거.
DROP TABLE quiz_options;
ALTER TABLE quiz_questions DROP COLUMN question;
ALTER TABLE quiz_questions DROP COLUMN explanation;
