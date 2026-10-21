-- ============================================================
-- 酷OJ 数据库变更脚本 V2
-- 执行前请备份数据库
-- ============================================================

-- 1. question 表：新增审核相关字段
ALTER TABLE question ADD COLUMN review_status INT NOT NULL DEFAULT 1 COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝' AFTER isPublic;
ALTER TABLE question ADD COLUMN review_time DATETIME NULL COMMENT '审核时间' AFTER review_status;
ALTER TABLE question ADD COLUMN review_user_id BIGINT NULL COMMENT '审核人ID' AFTER review_time;
ALTER TABLE question ADD COLUMN review_remark VARCHAR(512) NULL COMMENT '审核备注' AFTER review_user_id;

-- 2. contest 表：新增可见性和允许团队字段
ALTER TABLE contest ADD COLUMN visibility INT NOT NULL DEFAULT 0 COMMENT '可见性：0-公开，1-私有' AFTER contestType;
ALTER TABLE contest ADD COLUMN allowed_team_ids VARCHAR(1024) NULL COMMENT '允许参赛的团队ID列表（JSON数组，私有比赛用）' AFTER visibility;

-- 3. user 表：userName 唯一索引（先清理重复数据）
-- 注意：执行前请先检查是否有重复 userName
-- SELECT userName, COUNT(*) as cnt FROM user WHERE userName IS NOT NULL AND userName != '' GROUP BY userName HAVING cnt > 1;
-- 如果有重复，需要先手动处理（例如给重复的用户名加后缀）
-- 然后执行：
-- ALTER TABLE user ADD UNIQUE INDEX uk_userName (userName);

-- 4. 为 question 表的 review_status 添加索引（审核列表查询用）
ALTER TABLE question ADD INDEX idx_review_status (review_status);

-- 5. 为 contest 表的 visibility 添加索引
ALTER TABLE contest ADD INDEX idx_visibility (visibility);
