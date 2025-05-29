-- TechDaily 数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS techdaily CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE techdaily;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(191) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(191),
    role VARCHAR(50) DEFAULT 'USER',
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建文章表
CREATE TABLE IF NOT EXISTS articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(191) NOT NULL,
    title_zh VARCHAR(191),
    url VARCHAR(500) NOT NULL,
    content TEXT,
    summary TEXT,
    summary_zh TEXT,
    author VARCHAR(191),
    source VARCHAR(100) NOT NULL,
    tags VARCHAR(191),
    publish_time TIMESTAMP,
    views INT DEFAULT 0,
    likes INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PUBLISHED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_publish_time (publish_time),
    INDEX idx_source (source),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- 创建日报表
CREATE TABLE IF NOT EXISTS daily_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_date DATE NOT NULL UNIQUE,
    title VARCHAR(191) NOT NULL,
    summary TEXT,
    content TEXT,
    highlights TEXT,
    trends TEXT,
    article_ids TEXT,
    total_articles INT DEFAULT 0,
    read_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'DRAFT',
    generated_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_report_date (report_date),
    INDEX idx_status (status),
    INDEX idx_generated_at (generated_at)
);

-- 插入默认管理员用户
INSERT IGNORE INTO users (username, password, email, role, enabled) VALUES 
('admin', '$2a$10$U4PgANfJv1On3TuCzHVVJO8XssbJlN2XK9tuZH150fzO5/P0B7z0.', 'admin@techdaily.com', 'ADMIN', TRUE);

-- 插入示例文章数据
INSERT IGNORE INTO articles (title, title_zh, url, content, summary, summary_zh, author, source, tags, publish_time, views, likes, status) VALUES 
('React 18 New Features and Performance Improvements', 'React 18 新特性和性能改进', 'https://react.dev/blog/2024/04/25/react-19', 
 'React 18 introduces several new features including automatic batching, new APIs, and performance improvements...', 
 'React 18 brings automatic batching, concurrent features, and new APIs for better performance and user experience.',
 'React 18 带来了自动批处理、并发特性和新的 API，提供更好的性能和用户体验。',
 'React Team', 'React Official', 'React,JavaScript,Frontend', NOW() - INTERVAL 1 DAY, 156, 23, 'PUBLISHED'),

('Vue 3.4 Composition API Enhancements', 'Vue 3.4 组合式 API 增强', 'https://vuejs.org/guide/extras/composition-api-faq.html',
 'Vue 3.4 introduces new composition API features and better TypeScript support...',
 'Vue 3.4 enhances the Composition API with better reactivity and TypeScript integration.',
 'Vue 3.4 通过更好的响应式和 TypeScript 集成增强了组合式 API。',
 'Evan You', 'Vue.js', 'Vue,JavaScript,Frontend', NOW() - INTERVAL 1 DAY, 134, 19, 'PUBLISHED'),

('Node.js 20 LTS Release Notes', 'Node.js 20 LTS 发布说明', 'https://nodejs.org/en/blog/release/v20.0.0',
 'Node.js 20 LTS brings performance improvements, new APIs, and enhanced security features...',
 'Node.js 20 LTS includes performance boosts, new experimental features, and security enhancements.',
 'Node.js 20 LTS 包含性能提升、新的实验性功能和安全增强。',
 'Node.js Team', 'Node.js', 'Node.js,JavaScript,Backend', NOW() - INTERVAL 1 DAY, 198, 31, 'PUBLISHED'),

('Python 3.12 Performance Benchmarks', 'Python 3.12 性能基准测试', 'https://docs.python.org/3.12/whatsnew/3.12.html',
 'Python 3.12 shows significant performance improvements across various benchmarks...',
 'Python 3.12 demonstrates notable speed improvements and new language features.',
 'Python 3.12 展示了显著的速度改进和新的语言特性。',
 'Python Core Team', 'Python.org', 'Python,Performance,Programming', NOW() - INTERVAL 1 DAY, 167, 28, 'PUBLISHED'),

('Docker Container Security Best Practices', 'Docker 容器安全最佳实践', 'https://docs.docker.com/engine/security/',
 'Learn essential security practices for Docker containers in production environments...',
 'Essential security guidelines for running Docker containers safely in production.',
 '在生产环境中安全运行 Docker 容器的基本安全指南。',
 'Docker Security Team', 'Docker', 'Docker,Security,DevOps', NOW() - INTERVAL 1 DAY, 145, 22, 'PUBLISHED'),

('Kubernetes 1.29 New Features', 'Kubernetes 1.29 新特性', 'https://kubernetes.io/blog/2023/12/13/kubernetes-v1-29-release/',
 'Kubernetes 1.29 introduces new features for better cluster management and security...',
 'Kubernetes 1.29 brings enhanced cluster management capabilities and security improvements.',
 'Kubernetes 1.29 带来了增强的集群管理能力和安全改进。',
 'Kubernetes SIG Release', 'Kubernetes', 'Kubernetes,DevOps,Cloud', NOW() - INTERVAL 1 DAY, 189, 35, 'PUBLISHED'),

('AI-Powered Code Generation with GitHub Copilot', 'GitHub Copilot 的 AI 代码生成', 'https://github.blog/2023-06-20-how-to-write-better-prompts-for-github-copilot/',
 'Explore how AI-powered code generation is transforming software development workflows...',
 'GitHub Copilot revolutionizes coding with AI-powered suggestions and code completion.',
 'GitHub Copilot 通过 AI 驱动的建议和代码补全革命性地改变了编程。',
 'GitHub Team', 'GitHub', 'AI,GitHub,Programming', NOW() - INTERVAL 1 DAY, 223, 41, 'PUBLISHED'),

('WebAssembly Performance in Modern Browsers', '现代浏览器中的 WebAssembly 性能', 'https://webassembly.org/docs/use-cases/',
 'Analysis of WebAssembly performance improvements across different browser engines...',
 'WebAssembly delivers near-native performance for web applications across all major browsers.',
 'WebAssembly 为所有主要浏览器的 Web 应用程序提供接近原生的性能。',
 'WebAssembly Community', 'WebAssembly.org', 'WebAssembly,Performance,Web', NOW() - INTERVAL 1 DAY, 112, 18, 'PUBLISHED');

-- 插入示例日报数据
INSERT IGNORE INTO daily_reports (report_date, title, summary, content, highlights, trends, article_ids, total_articles, read_count, status, generated_at) VALUES 
(CURDATE(), CONCAT(CURDATE(), ' 技术日报'), 
 '今日技术要点：React 18 新特性发布，Vue 3.4 组合式 API 增强，Node.js 20 LTS 正式发布，以及 AI 代码生成工具的最新进展。',
 '## 📰 今日技术日报

### 🔥 热门技术动态

**前端框架更新**
- React 18 发布新特性，包含自动批处理和并发特性
- Vue 3.4 增强组合式 API，提供更好的 TypeScript 支持

**后端技术进展**  
- Node.js 20 LTS 正式发布，带来性能提升和安全增强
- Python 3.12 性能基准测试显示显著改进

**DevOps 与云原生**
- Docker 容器安全最佳实践指南更新
- Kubernetes 1.29 引入新的集群管理功能

**人工智能**
- GitHub Copilot AI 代码生成能力持续改进
- WebAssembly 在现代浏览器中的性能表现优异

### 📊 今日统计
- 采集文章：8 篇
- 涵盖技术：React, Vue, Node.js, Python, Docker, Kubernetes, AI, WebAssembly
- 主要来源：官方博客、技术文档、社区分享

### 🎯 技术趋势
1. 前端框架持续优化性能和开发体验
2. AI 辅助编程工具日趋成熟
3. 云原生技术栈不断完善
4. WebAssembly 应用场景扩大',
 '热门技术关键词：React, Vue, Node.js, Python, Docker, Kubernetes, AI, WebAssembly',
 '主要信息源：React Official(1篇) Vue.js(1篇) Node.js(1篇) Python.org(1篇) Docker(1篇) Kubernetes(1篇) GitHub(1篇) WebAssembly.org(1篇)',
 '1,2,3,4,5,6,7,8', 8, 0, 'PUBLISHED', NOW());

COMMIT;

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

-- 创建数据库用户 (生产环境建议使用独立用户)
-- CREATE USER IF NOT EXISTS 'techdaily'@'%' IDENTIFIED BY 'TechDaily2024!';
-- GRANT ALL PRIVILEGES ON techdaily.* TO 'techdaily'@'%';
-- FLUSH PRIVILEGES;

-- 显示创建结果
SELECT 'Database initialization completed!' as status;
SELECT COUNT(*) as user_count FROM users;
SELECT COUNT(*) as article_count FROM articles; 