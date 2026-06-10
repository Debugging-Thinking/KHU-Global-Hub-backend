-- 관리자 계정 플래그. 단일 운영자 계정(ADMIN_EMAIL)에만 true.
-- is_admin=true면 모든 게시물/댓글/Q&A/강의평 삭제 + 멘토링 수동매칭/전체현황 권한.
ALTER TABLE members ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT false;
