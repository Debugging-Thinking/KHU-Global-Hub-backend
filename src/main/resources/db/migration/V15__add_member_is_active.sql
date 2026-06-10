-- 회원 활동 정지 플래그. false면 관리자가 정지한 계정 → 로그인 차단.
ALTER TABLE members ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true;
