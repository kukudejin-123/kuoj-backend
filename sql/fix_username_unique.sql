-- ============================================================
-- 酷OJ 用户名唯一性数据迁移脚本
-- 用途：为历史用户补填 userName，并添加唯一索引兜底
-- 执行前请务必备份数据库！
-- ============================================================

-- 步骤 1：查看当前缺失用户名的用户（先执行这个查询确认影响范围）
SELECT id, userAccount, userName FROM user WHERE userName IS NULL OR userName = '';

-- 步骤 2：为 userName 为空的所有用户，用 userAccount 填充
UPDATE user SET userName = userAccount WHERE userName IS NULL OR userName = '';

-- 步骤 3：处理 userName 重复的用户（保留最早创建的，其余加数字后缀）
-- 先查看有哪些重复
SELECT userName, COUNT(*) as cnt FROM user WHERE userName IS NOT NULL AND userName != '' GROUP BY userName HAVING cnt > 1;

-- 对重复的用户名，保留 id 最小的，其余追加 _1, _2 ... 后缀
-- 注意：MySQL 不支持直接窗口函数（5.7），用变量模拟行号
UPDATE user u
JOIN (
    SELECT id,
           userName,
           @rn := IF(@prev = userName, @rn + 1, 1) AS rn,
           @prev := userName
    FROM user
    CROSS JOIN (SELECT @prev := NULL, @rn := 0) vars
    WHERE userName IS NOT NULL AND userName != ''
    ORDER BY userName, id
) ranked ON u.id = ranked.id
SET u.userName = CONCAT(ranked.userName, '_', ranked.rn)
WHERE ranked.rn > 1;

-- 步骤 4：确认无重复、无空值后再添加唯一索引（失败说明还有重复数据，需人工处理）
ALTER TABLE user ADD UNIQUE INDEX uk_userName (userName);

-- 步骤 5：验证
SELECT id, userAccount, userName FROM user ORDER BY id LIMIT 20;
SHOW INDEX FROM user WHERE Key_name = 'uk_userName';
