-- 강의평 사전번역(게시판 방식) — 강의평 1개당 언어별 1행(최대 6).
CREATE TABLE course_review_translations (
    id         BIGSERIAL PRIMARY KEY,
    review_id  BIGINT NOT NULL REFERENCES course_reviews(id) ON DELETE CASCADE,
    language   VARCHAR(20) NOT NULL,
    content    TEXT NOT NULL,
    CONSTRAINT uq_course_review_translation UNIQUE (review_id, language)
);

CREATE INDEX idx_course_review_translations_review ON course_review_translations(review_id);
