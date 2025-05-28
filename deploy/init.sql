-- TechDaily 数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS techdaily CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE techdaily;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    email VARCHAR(255) DEFAULT '' COMMENT '邮箱',
    role VARCHAR(50) DEFAULT 'ADMIN' COMMENT '角色(ADMIN/USER)',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_login TIMESTAMP NULL COMMENT '最后登录时间',
    INDEX idx_username (username),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 创建文章表
CREATE TABLE IF NOT EXISTS articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL COMMENT '标题',
    title_zh VARCHAR(500) DEFAULT '' COMMENT '中文标题',
    url VARCHAR(1000) UNIQUE NOT NULL COMMENT '文章链接',
    author VARCHAR(255) DEFAULT '' COMMENT '作者',
    content TEXT COMMENT '内容',
    content_zh TEXT COMMENT '中文内容',
    summary TEXT COMMENT '摘要',
    summary_zh TEXT COMMENT '中文摘要',
    source VARCHAR(100) NOT NULL COMMENT '来源',
    tags VARCHAR(500) DEFAULT '' COMMENT '标签',
    publish_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    crawl_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '爬取时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    status VARCHAR(50) DEFAULT 'PUBLISHED' COMMENT '状态',
    views INT DEFAULT 0 COMMENT '浏览量',
    likes INT DEFAULT 0 COMMENT '点赞数',
    INDEX idx_source (source),
    INDEX idx_publish_time (publish_time),
    INDEX idx_status (status),
    INDEX idx_url (url)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

-- 创建爬虫记录表
CREATE TABLE IF NOT EXISTS crawl_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source VARCHAR(100) NOT NULL COMMENT '爬虫源',
    crawl_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '爬取时间',
    total_crawled INT DEFAULT 0 COMMENT '总爬取数',
    success_count INT DEFAULT 0 COMMENT '成功数',
    error_count INT DEFAULT 0 COMMENT '失败数',
    status VARCHAR(50) DEFAULT 'SUCCESS' COMMENT '状态',
    error_message TEXT COMMENT '错误信息',
    INDEX idx_source (source),
    INDEX idx_crawl_time (crawl_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='爬虫记录表';

-- 创建日报表
CREATE TABLE IF NOT EXISTS daily_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_date DATE UNIQUE NOT NULL COMMENT '日报日期',
    title VARCHAR(500) NOT NULL COMMENT '日报标题',
    content TEXT NOT NULL COMMENT '日报内容',
    summary TEXT COMMENT '摘要',
    article_count INT DEFAULT 0 COMMENT '文章数量',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    status VARCHAR(50) DEFAULT 'PUBLISHED' COMMENT '状态',
    INDEX idx_report_date (report_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日报表';

-- 插入默认管理员用户 (密码: 111111)
INSERT INTO users (username, password, email, role, enabled, created_at) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9tYjKUiFjFO2/fG', 'admin@techdaily.com', 'ADMIN', TRUE, NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password);

-- 插入示例文章数据
INSERT INTO articles (title, url, author, content, source, tags, publish_time, status) VALUES
('Spring Boot 3.0 新特性详解', 'https://example.com/spring-boot-3', 'Spring Team', 'Spring Boot 3.0 带来了许多令人兴奋的新特性...', 'Spring Blog', 'Spring,Java,框架', NOW(), 'PUBLISHED'),
('React 18 并发特性深度解析', 'https://example.com/react-18', 'React Team', 'React 18 引入了并发渲染等重要特性...', 'React Blog', 'React,JavaScript,前端', NOW(), 'PUBLISHED'),
('MySQL 8.0 性能优化最佳实践', 'https://example.com/mysql-8', 'MySQL Team', 'MySQL 8.0 在性能方面有了显著提升...', 'MySQL Blog', 'MySQL,数据库,性能', NOW(), 'PUBLISHED')
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- 创建数据库用户 (生产环境建议使用独立用户)
-- CREATE USER IF NOT EXISTS 'techdaily'@'%' IDENTIFIED BY 'TechDaily2024!';
-- GRANT ALL PRIVILEGES ON techdaily.* TO 'techdaily'@'%';
-- FLUSH PRIVILEGES;

-- 显示创建结果
SELECT 'Database initialization completed!' as status;
SELECT COUNT(*) as user_count FROM users;
SELECT COUNT(*) as article_count FROM articles; 