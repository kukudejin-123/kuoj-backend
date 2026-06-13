-- OJ系统团队功能数据库表创建脚本
-- 请在MySQL数据库中执行此脚本

-- 团队表
CREATE TABLE IF NOT EXISTS team (
    id BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    teamName VARCHAR(256) NOT NULL COMMENT '团队名称',
    teamDesc VARCHAR(1024) COMMENT '团队描述',
    teamAvatar VARCHAR(1024) COMMENT '团队头像',
    userId BIGINT NOT NULL COMMENT '创建者/队长用户id',
    memberCount INT DEFAULT 1 NOT NULL COMMENT '成员数量',
    maxMemberCount INT DEFAULT 50 NOT NULL COMMENT '最大成员数量',
    status TINYINT DEFAULT 0 NOT NULL COMMENT '状态：0-正常，1-禁用',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT DEFAULT 0 NOT NULL,
    INDEX idx_userId (userId),
    INDEX idx_teamName (teamName)
) COMMENT '团队表' COLLATE = utf8mb4_unicode_ci;

-- 团队用户关联表
CREATE TABLE IF NOT EXISTS team_user (
    id BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    teamId BIGINT NOT NULL COMMENT '团队id',
    userId BIGINT NOT NULL COMMENT '用户id',
    userRole TINYINT DEFAULT 0 NOT NULL COMMENT '角色：0-普通成员，1-管理员，2-队长',
    joinTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT DEFAULT 0 NOT NULL,
    UNIQUE KEY uk_team_user (teamId, userId),
    INDEX idx_teamId (teamId),
    INDEX idx_userId (userId)
) COMMENT '团队用户关联表' COLLATE = utf8mb4_unicode_ci;