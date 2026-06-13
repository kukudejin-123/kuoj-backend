-- 在 question 表增加 SPJ 相关字段
-- 执行时间：2026-06-13
-- 说明：支持 SPJ（Special Judge）自定义判题功能

ALTER TABLE question
ADD COLUMN spj_code TEXT COMMENT 'SPJ判题程序代码',
ADD COLUMN spj_language VARCHAR(20) DEFAULT 'java' COMMENT 'SPJ程序语言',
ADD COLUMN spj_compiled TINYINT(1) DEFAULT 0 COMMENT 'SPJ是否已预编译';

-- 为已有题目设置默认值
UPDATE question SET spj_language = 'java', spj_compiled = 0 WHERE spj_language IS NULL;
