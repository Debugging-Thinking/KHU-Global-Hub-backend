CREATE TABLE member_badges (
    id        BIGSERIAL PRIMARY KEY,
    member_id BIGINT      NOT NULL,
    badge_id  VARCHAR(50) NOT NULL,
    earned_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_member_badge UNIQUE (member_id, badge_id)
);
