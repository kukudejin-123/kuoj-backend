-- OJ系统数据库索引优化
-- 执行此文件可添加缺失的索引，提升查询性能

-- 题目表索引优化
-- 题目标题搜索索引
CREATE INDEX idx_title ON question(title);

-- 题目提交表索引优化
-- 题目ID索引（查询某题的所有提交）
CREATE INDEX idx_questionId ON question_submit(questionId);

-- 题目ID和用户ID复合索引（查询某用户在某题的提交记录）
CREATE INDEX idx_questionId_userId ON question_submit(questionId, userId);

-- 创建时间索引（按时间排序查询）
CREATE INDEX idx_createTime ON question_submit(createTime);

-- 帖子表索引优化
-- 帖子标题搜索索引
CREATE INDEX idx_post_title ON post(title);

-- 验证索引是否创建成功
SHOW INDEX FROM question;
SHOW INDEX FROM question_submit;
SHOW INDEX FROM user;
SHOW INDEX FROM post;