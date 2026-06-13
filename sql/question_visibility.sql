-- 修改question表，添加isPublic和contestId字段
ALTER TABLE question ADD COLUMN isPublic TINYINT DEFAULT 1 NOT NULL COMMENT '是否公开：1-公开，0-仅比赛可见';
ALTER TABLE question ADD COLUMN contestId BIGINT COMMENT '关联比赛id（仅比赛题目）';
ALTER TABLE question ADD INDEX idx_isPublic (isPublic);
ALTER TABLE question ADD INDEX idx_contestId (contestId);