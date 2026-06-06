-- 강의평 기능: 강의(lectures) + 강의평(course_reviews).
CREATE TABLE lectures (
    id        BIGSERIAL PRIMARY KEY,
    code      VARCHAR(50)  NOT NULL,
    name      VARCHAR(200) NOT NULL,
    professor VARCHAR(100) NOT NULL,
    college   VARCHAR(100),
    type      VARCHAR(50),
    credits   INTEGER,
    semester  VARCHAR(20)  NOT NULL
);

CREATE TABLE course_reviews (
    id         BIGSERIAL PRIMARY KEY,
    lecture_id BIGINT NOT NULL REFERENCES lectures(id) ON DELETE CASCADE,
    author_id  BIGINT NOT NULL,
    rating     INTEGER NOT NULL,
    content    TEXT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_course_reviews_lecture ON course_reviews(lecture_id);
CREATE INDEX idx_lectures_semester ON lectures(semester);
