-- ================================================================
-- KHU Global Hub - ERD Cloud 임포트용 DDL (PostgreSQL)
-- ERD Cloud (erdcloud.com) → 새 프로젝트 → Import DDL에 붙여넣기
-- 최종 수정: 2026-04-10 (엔티티 코드 기준 재작성)
-- ================================================================

-- 인증 전용. 로그인/회원가입 정보만 관리.
CREATE TABLE members (
    id                        BIGSERIAL     PRIMARY KEY,
    email                     VARCHAR(255)  NOT NULL UNIQUE,  -- 로그인 이메일 (@khu.ac.kr만 허용)
    password                  VARCHAR(255)  NOT NULL,         -- BCrypt 해시 비밀번호
    is_email_verified         BOOLEAN       NOT NULL DEFAULT FALSE,
    email_verification_code   VARCHAR(10),                    -- 인증 코드 (인증 완료 후 NULL)
    code_expired_at           TIMESTAMP,                      -- 인증 코드 만료 시각
    refresh_token             VARCHAR(500),                   -- JWT Refresh Token (로그아웃 시 NULL)
    refresh_token_expired_at  TIMESTAMP,                      -- Refresh Token 만료 시각
    created_at                TIMESTAMP,
    updated_at                TIMESTAMP
);

-- 프로필 정보. members와 1:1 관계.
-- profiles row가 존재하면 프로필 설정 완료, 없으면 미완성.
CREATE TABLE profiles (
    id              BIGSERIAL    PRIMARY KEY,
    member_id       BIGINT       NOT NULL UNIQUE,             -- 연결된 회원 (1:1)
    name            VARCHAR(100) NOT NULL,                    -- 실명
    profile_image   VARCHAR(500),                             -- AWS S3 이미지 URL (nullable)
    department      VARCHAR(100) NOT NULL,                    -- 학과
    nationality     VARCHAR(100) NOT NULL,                    -- 국적
    admission_year  INT          NOT NULL,                    -- 입학년도 (예: 2024)
    language        VARCHAR(10)  NOT NULL,                    -- 선택 언어 (KO/EN/ZH/VI/ES/MN)
    mentoring_role  VARCHAR(20)  NOT NULL DEFAULT 'MENTEE',   -- MENTOR/MENTEE/NONE
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id)
);

CREATE TABLE posts (
    id             BIGSERIAL    PRIMARY KEY,
    author_id      BIGINT       NOT NULL,                     -- 작성자 (익명이라도 DB엔 저장)
    board_type     VARCHAR(20)  NOT NULL,                     -- 게시판 종류 (FRESHMAN/FREE/GRADUATE)
    is_anonymous   BOOLEAN      NOT NULL,                     -- 익명 여부
    like_count     INT          NOT NULL DEFAULT 0,           -- 좋아요 수 (10 이상이면 인기 게시물)
    comment_count  INT          NOT NULL DEFAULT 0,           -- 댓글+대댓글 수 (비정규화)
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES members(id)
);

CREATE TABLE post_translations (
    id        BIGSERIAL     PRIMARY KEY,
    post_id   BIGINT        NOT NULL,                         -- 원본 게시글
    language  VARCHAR(10)   NOT NULL,                         -- 언어 코드 (KO/EN/ZH/VI/ES/MN)
    title     VARCHAR(500)  NOT NULL,                         -- 번역된 제목
    content   TEXT          NOT NULL,                         -- 번역된 본문
    UNIQUE (post_id, language),
    FOREIGN KEY (post_id) REFERENCES posts(id)
);

CREATE TABLE post_images (
    id           BIGSERIAL     PRIMARY KEY,
    post_id      BIGINT        NOT NULL,                      -- 속한 게시글
    image_url    VARCHAR(500)  NOT NULL,                      -- AWS S3 URL
    order_index  INT           NOT NULL,                      -- 이미지 표시 순서 (0부터)
    FOREIGN KEY (post_id) REFERENCES posts(id)
);

CREATE TABLE post_likes (
    id          BIGSERIAL  PRIMARY KEY,
    member_id   BIGINT     NOT NULL,                          -- 좋아요 누른 유저
    post_id     BIGINT     NOT NULL,                          -- 좋아요 받은 게시글
    created_at  TIMESTAMP,
    UNIQUE (member_id, post_id),
    FOREIGN KEY (member_id) REFERENCES members(id),
    FOREIGN KEY (post_id)   REFERENCES posts(id)
);

CREATE TABLE qnas (
    id            BIGSERIAL  PRIMARY KEY,
    author_id     BIGINT     NOT NULL,                        -- 질문 작성자 (익명이라도 DB엔 저장)
    is_anonymous  BOOLEAN    NOT NULL,                        -- 익명 여부
    is_adopted    BOOLEAN    NOT NULL DEFAULT FALSE,          -- 채택 완료 여부 (true이면 재채택 불가)
    like_count    INT        NOT NULL DEFAULT 0,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES members(id)
);

CREATE TABLE qna_translations (
    id        BIGSERIAL     PRIMARY KEY,
    qna_id    BIGINT        NOT NULL,                         -- 원본 질문
    language  VARCHAR(10)   NOT NULL,
    title     VARCHAR(500)  NOT NULL,                         -- 번역된 질문 제목
    content   TEXT          NOT NULL,                         -- 번역된 질문 본문
    UNIQUE (qna_id, language),
    FOREIGN KEY (qna_id) REFERENCES qnas(id)
);

CREATE TABLE qna_likes (
    id          BIGSERIAL  PRIMARY KEY,
    member_id   BIGINT     NOT NULL,
    qna_id      BIGINT     NOT NULL,
    created_at  TIMESTAMP,
    UNIQUE (member_id, qna_id),
    FOREIGN KEY (member_id) REFERENCES members(id),
    FOREIGN KEY (qna_id)    REFERENCES qnas(id)
);

CREATE TABLE answers (
    id            BIGSERIAL  PRIMARY KEY,
    qna_id        BIGINT     NOT NULL,                        -- 속한 질문
    author_id     BIGINT     NOT NULL,                        -- 답변 작성자
    is_anonymous  BOOLEAN    NOT NULL,                        -- 익명 여부
    is_adopted    BOOLEAN    NOT NULL DEFAULT FALSE,          -- 채택된 답변 여부
    like_count    INT        NOT NULL DEFAULT 0,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    FOREIGN KEY (qna_id)    REFERENCES qnas(id),
    FOREIGN KEY (author_id) REFERENCES members(id)
);

CREATE TABLE answer_translations (
    id         BIGSERIAL  PRIMARY KEY,
    answer_id  BIGINT     NOT NULL,                           -- 원본 답변
    language   VARCHAR(10) NOT NULL,
    content    TEXT        NOT NULL,                          -- 번역된 답변 본문
    UNIQUE (answer_id, language),
    FOREIGN KEY (answer_id) REFERENCES answers(id)
);

CREATE TABLE answer_likes (
    id          BIGSERIAL  PRIMARY KEY,
    member_id   BIGINT     NOT NULL,
    answer_id   BIGINT     NOT NULL,
    created_at  TIMESTAMP,
    UNIQUE (member_id, answer_id),
    FOREIGN KEY (member_id)  REFERENCES members(id),
    FOREIGN KEY (answer_id)  REFERENCES answers(id)
);

-- 통합 댓글 테이블.
-- target_type + target_id 조합으로 어느 게시물의 댓글인지 식별.
-- POST: posts.id, QNA: qnas.id, ANSWER: answers.id
CREATE TABLE comments (
    id            BIGSERIAL   PRIMARY KEY,
    target_type   VARCHAR(20) NOT NULL,                       -- 댓글 대상 종류 (POST/QNA/ANSWER)
    target_id     BIGINT      NOT NULL,                       -- 댓글 대상 ID
    parent_id     BIGINT      NULL,                           -- 부모 댓글 ID (NULL=댓글, 있으면=대댓글)
    author_id     BIGINT      NOT NULL,                       -- 댓글 작성자
    is_anonymous  BOOLEAN     NOT NULL,                       -- 익명 여부
    like_count    INT         NOT NULL DEFAULT 0,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    FOREIGN KEY (parent_id)  REFERENCES comments(id),
    FOREIGN KEY (author_id)  REFERENCES members(id)
);

CREATE TABLE comment_translations (
    id          BIGSERIAL   PRIMARY KEY,
    comment_id  BIGINT      NOT NULL,                         -- 원본 댓글
    language    VARCHAR(10) NOT NULL,
    content     TEXT        NOT NULL,                         -- 번역된 댓글 내용
    UNIQUE (comment_id, language),
    FOREIGN KEY (comment_id) REFERENCES comments(id)
);

CREATE TABLE comment_likes (
    id          BIGSERIAL  PRIMARY KEY,
    member_id   BIGINT     NOT NULL,
    comment_id  BIGINT     NOT NULL,
    created_at  TIMESTAMP,
    UNIQUE (member_id, comment_id),
    FOREIGN KEY (member_id)   REFERENCES members(id),
    FOREIGN KEY (comment_id)  REFERENCES comments(id)
);

CREATE TABLE mentor_mentee_matches (
    id          BIGSERIAL    PRIMARY KEY,
    mentor_id   BIGINT       NOT NULL,                        -- 멘토 유저
    mentee_id   BIGINT       NOT NULL,                        -- 멘티 유저
    semester    VARCHAR(20)  NOT NULL,                        -- 매칭 학기 (예: 2025-1)
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',       -- ACTIVE/COMPLETED/CANCELLED
    matched_at  TIMESTAMP,                                    -- 매칭 생성 시각
    FOREIGN KEY (mentor_id) REFERENCES members(id),
    FOREIGN KEY (mentee_id) REFERENCES members(id)
);

-- chat_rooms 테이블 없음.
-- DM 방식으로 sender_id + receiver_id 조합으로 대화를 식별한다.
-- 시스템 메시지는 sender_id = NULL, is_system = TRUE.
-- context_partner_id: 시스템 메시지가 어느 대화에 속하는지 특정하기 위해 사용.
--
-- A-B 대화 조회 조건:
--   (sender_id=A AND receiver_id=B)
--   OR (sender_id=B AND receiver_id=A)
--   OR (is_system=TRUE AND receiver_id=A AND context_partner_id=B)
--   OR (is_system=TRUE AND receiver_id=B AND context_partner_id=A)
CREATE TABLE chat_messages (
    id                  BIGSERIAL   PRIMARY KEY,
    sender_id           BIGINT      NULL,                     -- 발신자 (시스템 메시지면 NULL)
    receiver_id         BIGINT      NOT NULL,                 -- 수신자
    context_partner_id  BIGINT      NULL,                     -- 시스템 메시지 전용: 대화 상대 식별용
    content             TEXT        NOT NULL,                 -- 메시지 내용
    is_system           BOOLEAN     NOT NULL DEFAULT FALSE,   -- 시스템 자동 메시지 여부
    is_read             BOOLEAN     NOT NULL DEFAULT FALSE,   -- 읽음 여부
    sent_at             TIMESTAMP,                            -- 전송 시각
    FOREIGN KEY (sender_id)          REFERENCES members(id),
    FOREIGN KEY (receiver_id)        REFERENCES members(id),
    FOREIGN KEY (context_partner_id) REFERENCES members(id)
);
