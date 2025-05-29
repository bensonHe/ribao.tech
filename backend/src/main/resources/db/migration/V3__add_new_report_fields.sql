-- 添加新的日报格式字段
ALTER TABLE daily_reports 
ADD COLUMN today_trends TEXT COMMENT '今日趋势（200字以内）',
ADD COLUMN recommended_articles TEXT COMMENT '今日优质文章推荐（JSON格式存储）',
ADD COLUMN daily_quote TEXT COMMENT '每日一语（100字以内）',
ADD COLUMN solar_term VARCHAR(50) COMMENT '当日节气';

-- 更新现有记录的注释
ALTER TABLE daily_reports 
MODIFY COLUMN summary TEXT COMMENT '日报摘要（保留兼容性）',
MODIFY COLUMN content TEXT COMMENT '完整日报内容（保留兼容性）',
MODIFY COLUMN highlights TEXT COMMENT '今日亮点（保留兼容性）',
MODIFY COLUMN trends TEXT COMMENT '技术趋势（保留兼容性）'; 