-- D4: 댓글은 board BC 전용 → target_type 컬럼 제거 (CommentTargetType enum 폐기).
-- target_id는 이제 항상 게시글(post) ID를 가리킨다.

ALTER TABLE comments DROP COLUMN target_type;
