-- 如果之前没有添加过 isPublic 字段，先执行这个
-- ALTER TABLE question ADD COLUMN isPublic TINYINT DEFAULT 1 NOT NULL COMMENT '是否公开：1-公开，0-仅比赛可见';

-- 更新现有题目的 isPublic 字段，将 null 值设置为 1（公开）
UPDATE question SET isPublic = 1 WHERE isPublic IS NULL;

-- 确认更新结果
SELECT id, title, isPublic FROM question;
