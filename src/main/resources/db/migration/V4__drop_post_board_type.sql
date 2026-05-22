-- D2: 게시판 1종(자유)으로 통합 → board_type 컬럼 제거.

ALTER TABLE posts DROP COLUMN board_type;
