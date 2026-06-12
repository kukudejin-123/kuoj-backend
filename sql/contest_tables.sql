-- OJ系统比赛功能数据库表创建脚本
-- 请在MySQL数据库中执行此脚本

-- 比赛表
CREATE TABLE IF NOT EXISTS contest (
    id BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    contestName VARCHAR(256) NOT NULL COMMENT '比赛名称',
    contestDesc VARCHAR(2048) COMMENT '比赛描述',
    userId BIGINT NOT NULL COMMENT '创建者用户id',
    startTime DATETIME NOT NULL COMMENT '开始时间',
    endTime DATETIME NOT NULL COMMENT '结束时间',
    contestType TINYINT DEFAULT 0 NOT NULL COMMENT '比赛类型：0-ACM，1-IOI',
    status TINYINT DEFAULT 0 NOT NULL COMMENT '状态：0-未开始，1-进行中，2-已结束',
    participantCount INT DEFAULT 0 NOT NULL COMMENT '参赛人数',
    questionCount INT DEFAULT 0 NOT NULL COMMENT '题目数量',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT DEFAULT 0 NOT NULL,
    INDEX idx_userId (userId),
    INDEX idx_startTime (startTime),
    INDEX idx_status (status)
) COMMENT '比赛表' COLLATE = utf8mb4_unicode_ci;

-- 比赛题目关联表
CREATE TABLE IF NOT EXISTS contest_question (
    id BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    contestId BIGINT NOT NULL COMMENT '比赛id',
    questionId BIGINT NOT NULL COMMENT '题目id',
    questionOrder INT DEFAULT 0 NOT NULL COMMENT '题目顺序（A=0, B=1...）',
    score INT DEFAULT 100 NOT NULL COMMENT '题目分值（IOI模式）',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT DEFAULT 0 NOT NULL,
    UNIQUE KEY uk_contest_question (contestId, questionId),
    INDEX idx_contestId (contestId)
) COMMENT '比赛题目关联表' COLLATE = utf8mb4_unicode_ci;

-- 比赛提交表
CREATE TABLE IF NOT EXISTS contest_submit (
    id BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    contestId BIGINT NOT NULL COMMENT '比赛id',
    questionId BIGINT NOT NULL COMMENT '题目id',
    userId BIGINT NOT NULL COMMENT '用户id',
    language VARCHAR(256) NOT NULL COMMENT '编程语言',
    code TEXT NOT NULL COMMENT '提交代码',
    judgeInfo TEXT COMMENT '判题信息(JSON)',
    status VARCHAR(256) NOT NULL COMMENT '判题状态',
    submitTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '提交时间',
    isFirstAccept TINYINT DEFAULT 0 NOT NULL COMMENT '是否首次通过',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT DEFAULT 0 NOT NULL,
    INDEX idx_contestId (contestId),
    INDEX idx_questionId (questionId),
    INDEX idx_userId (userId),
    INDEX idx_contestUser (contestId, userId)
) COMMENT '比赛提交表' COLLATE = utf8mb4_unicode_ci;

-- 比赛参赛者表
CREATE TABLE IF NOT EXISTS contest_participant (
    id BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    contestId BIGINT NOT NULL COMMENT '比赛id',
    userId BIGINT NOT NULL COMMENT '用户id',
    joinTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '报名时间',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT DEFAULT 0 NOT NULL,
    UNIQUE KEY uk_contest_user (contestId, userId),
    INDEX idx_contestId (contestId)
) COMMENT '比赛参赛者表' COLLATE = utf8mb4_unicode_ci;