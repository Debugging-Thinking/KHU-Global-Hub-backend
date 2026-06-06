-- 댓글/Q&A/답변/채팅 메시지에 첨부 이미지 URL(선택) 컬럼 추가.
-- 게시글은 별도 post_images 테이블을 그대로 사용(다중 이미지). 나머지는 단일 이미지 URL.
ALTER TABLE comments       ADD COLUMN image_url VARCHAR(500);
ALTER TABLE qnas           ADD COLUMN image_url VARCHAR(500);
ALTER TABLE answers        ADD COLUMN image_url VARCHAR(500);
ALTER TABLE chat_messages  ADD COLUMN image_url VARCHAR(500);
