-- 添加题号字段
ALTER TABLE question ADD COLUMN questionNumber INT DEFAULT NULL COMMENT '题号';

-- 为现有题目自动分配题号（按创建时间排序）
SET @row_number = 1000;
UPDATE question
SET questionNumber = (@row_number := @row_number + 1)
WHERE isDelete = 0
ORDER BY createTime;

-- 添加唯一索引
ALTER TABLE question ADD UNIQUE INDEX idx_question_number (questionNumber);
