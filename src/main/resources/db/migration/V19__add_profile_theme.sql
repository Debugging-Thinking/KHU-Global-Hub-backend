-- 프로필 UI 테마 선호 추가 (LIGHT | DARK). 기본 LIGHT — 기존 행/미입력은 라이트 유지.
-- 운영 드리프트 대비 IF NOT EXISTS (이미 ddl-auto=update로 생성된 운영엔 무해 스킵).
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS theme VARCHAR(16) NOT NULL DEFAULT 'LIGHT';
