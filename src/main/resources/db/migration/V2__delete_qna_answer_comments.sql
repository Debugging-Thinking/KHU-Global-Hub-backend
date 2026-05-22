-- D3: QnA·답변 댓글 폐기 (generic Comment → board 전용 흡수).
-- ⚠️ 비가역 데이터 삭제. 운영 적용 전 RDS 스냅샷 필수.
-- 의존 행(좋아요·번역) 먼저 제거 후, 대댓글→댓글 순으로 삭제(parent_id 자기참조 FK 회피).

DELETE FROM comment_likes
 WHERE comment_id IN (SELECT id FROM comments WHERE target_type IN ('QNA', 'ANSWER'));

DELETE FROM comment_translations
 WHERE comment_id IN (SELECT id FROM comments WHERE target_type IN ('QNA', 'ANSWER'));

DELETE FROM comments WHERE target_type IN ('QNA', 'ANSWER') AND parent_id IS NOT NULL;
DELETE FROM comments WHERE target_type IN ('QNA', 'ANSWER');
