-- 日报表结构迁移脚本
-- 用于将现有的daily_reports表更新为与DailyReport实体类匹配的结构

USE techdaily;

-- 添加缺失的字段
ALTER TABLE daily_reports 
ADD COLUMN IF NOT EXISTS highlights TEXT COMMENT '今日亮点' AFTER content,
ADD COLUMN IF NOT EXISTS trends TEXT COMMENT '技术趋势' AFTER highlights,
ADD COLUMN IF NOT EXISTS article_ids TEXT COMMENT '关联的文章ID，逗号分隔' AFTER trends,
ADD COLUMN IF NOT EXISTS total_articles INT DEFAULT 0 COMMENT '总文章数' AFTER article_ids,
ADD COLUMN IF NOT EXISTS read_count INT DEFAULT 0 COMMENT '阅读次数' AFTER total_articles,
ADD COLUMN IF NOT EXISTS generated_at TIMESTAMP NULL COMMENT '生成时间' AFTER status;

-- 重命名字段（如果存在旧字段名）
-- 检查是否存在article_count字段，如果存在则重命名为total_articles
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = 'techdaily' 
     AND TABLE_NAME = 'daily_reports' 
     AND COLUMN_NAME = 'article_count') > 0,
    'ALTER TABLE daily_reports CHANGE article_count total_articles INT DEFAULT 0 COMMENT "总文章数"',
    'SELECT "article_count column does not exist" as info'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 更新status字段的默认值
ALTER TABLE daily_reports 
MODIFY COLUMN status VARCHAR(50) DEFAULT 'DRAFT' COMMENT '状态(DRAFT/GENERATING/PUBLISHED/ARCHIVED)';

-- 添加索引
ALTER TABLE daily_reports 
ADD INDEX IF NOT EXISTS idx_generated_at (generated_at);

-- 更新现有记录的generated_at字段（如果为空）
UPDATE daily_reports 
SET generated_at = created_at 
WHERE generated_at IS NULL;

-- 显示更新结果
SELECT 'Daily reports table migration completed!' as status;
SELECT COUNT(*) as total_reports FROM daily_reports;
DESCRIBE daily_reports; 