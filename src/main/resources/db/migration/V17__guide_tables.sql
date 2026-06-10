-- 학사 가이드 다국어 DB — 퀴즈(V16)와 동일한 사전번역 방식.
-- 카테고리/팁 메타 + 언어별 번역 행 분리. 신규 기능(기존 데이터 없음).
-- 텍스트(카테고리 title, 팁 title/content)는 언어별 행으로 분리, 메타(뱃지키/이모지/색/아이콘/링크/정렬)는 별도.

-- 1) 카테고리 메타.
CREATE TABLE guide_categories (
    id         BIGSERIAL PRIMARY KEY,
    badge_key  VARCHAR(50) NOT NULL,    -- 프론트 뱃지 식별 키 (예: COURSE_REG)
    emoji      VARCHAR(20),
    color      VARCHAR(20),
    sort_order INT NOT NULL DEFAULT 0
);

-- 2) 카테고리 번역 (카테고리 1개당 언어별 1행, 최대 6).
CREATE TABLE guide_category_translations (
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES guide_categories(id) ON DELETE CASCADE,
    language    VARCHAR(20) NOT NULL,
    title       TEXT NOT NULL,
    CONSTRAINT uq_guide_category_translation UNIQUE (category_id, language)
);

CREATE INDEX idx_guide_category_translations_category ON guide_category_translations(category_id);

-- 3) 팁 메타 (카테고리 하위 항목).
CREATE TABLE guide_tips (
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES guide_categories(id) ON DELETE CASCADE,
    icon        VARCHAR(20),
    link        TEXT,
    sort_order  INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_guide_tips_category ON guide_tips(category_id);

-- 4) 팁 번역 (팁 1개당 언어별 1행, 최대 6).
CREATE TABLE guide_tip_translations (
    id       BIGSERIAL PRIMARY KEY,
    tip_id   BIGINT NOT NULL REFERENCES guide_tips(id) ON DELETE CASCADE,
    language VARCHAR(20) NOT NULL,
    title    TEXT NOT NULL,
    content  TEXT NOT NULL,
    CONSTRAINT uq_guide_tip_translation UNIQUE (tip_id, language)
);

CREATE INDEX idx_guide_tip_translations_tip ON guide_tip_translations(tip_id);
