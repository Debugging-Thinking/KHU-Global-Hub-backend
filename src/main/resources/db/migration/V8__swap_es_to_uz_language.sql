-- 지원 언어 ES(스페인어) → UZ(우즈벡어) 교체.
-- KO/EN/ZH/VI/ES/MN → KO/EN/ZH/VI/UZ/MN. (MN 저장값은 그대로 'MN' — Azure 코드 mn-Cyrl 변경은 앱 레벨만)
-- ES는 데모 데이터만 존재하므로 정리한 뒤 CHECK 제약을 교체한다. 비가역(ES 번역행 삭제).

-- 1) 기존 ES 데이터 정리 (새 CHECK 제약 위반 방지)
DELETE FROM answer_translations  WHERE language = 'ES';
DELETE FROM comment_translations WHERE language = 'ES';
DELETE FROM post_translations    WHERE language = 'ES';
DELETE FROM qna_translations     WHERE language = 'ES';
UPDATE profiles SET language = 'EN' WHERE language = 'ES';

-- 2) CHECK 제약 ES→UZ 교체 (5개 테이블, 제약명은 V1 baseline 자동생성명)
ALTER TABLE answer_translations  DROP CONSTRAINT answer_translations_language_check,
    ADD  CONSTRAINT answer_translations_language_check  CHECK (language IN ('KO','EN','ZH','VI','UZ','MN'));
ALTER TABLE comment_translations DROP CONSTRAINT comment_translations_language_check,
    ADD  CONSTRAINT comment_translations_language_check CHECK (language IN ('KO','EN','ZH','VI','UZ','MN'));
ALTER TABLE post_translations    DROP CONSTRAINT post_translations_language_check,
    ADD  CONSTRAINT post_translations_language_check    CHECK (language IN ('KO','EN','ZH','VI','UZ','MN'));
ALTER TABLE profiles             DROP CONSTRAINT profiles_language_check,
    ADD  CONSTRAINT profiles_language_check             CHECK (language IN ('KO','EN','ZH','VI','UZ','MN'));
ALTER TABLE qna_translations     DROP CONSTRAINT qna_translations_language_check,
    ADD  CONSTRAINT qna_translations_language_check     CHECK (language IN ('KO','EN','ZH','VI','UZ','MN'));
