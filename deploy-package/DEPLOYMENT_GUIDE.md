# 技术日报系统部署指南 v1.1.0

## 🚀 系统概述

**TechDaily** 是一个智能技术日报生成系统，集成了6个高质量的国外技术源，通过AI分析生成每日技术报告。

### ✨ 新版本特性 (v1.1.0)
- 🔥 **GitHub Trending**：每日热门开源项目 Top 3
- 💼 **IBM Developer**：企业级技术内容 Top 3
- 📊 智能热度追踪和星标统计
- 🎯 精准内容控制，确保质量

## 📋 系统要求

### 基础环境
- **Java**: JDK 8+ (推荐 JDK 11)
- **MySQL**: 5.7+ (推荐 8.0+)
- **内存**: 最少 2GB RAM
- **存储**: 最少 10GB 可用空间
- **网络**: 稳定的国外网站访问能力

### 网络要求
由于采集器全部为国外源，需要确保以下网站的访问：
- ✅ GitHub.com
- ✅ news.ycombinator.com (Hacker News)
- ✅ dev.to
- ✅ infoq.com
- ✅ daily.dev
- ✅ developer.ibm.com

## 🛠️ 快速部署

### 1. 数据库准备
```sql
-- 创建数据库
CREATE DATABASE techdaily CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户（可选）
CREATE USER 'techdaily'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON techdaily.* TO 'techdaily'@'localhost';
FLUSH PRIVILEGES;
```

### 2. 配置文件
编辑 `application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/techdaily?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: techdaily
    password: your_password
    
# AI服务配置（阿里云通义千问）
ai:
  qwen:
    api-key: your_qwen_api_key
    model: qwen-turbo
```

### 3. 启动应用
```bash
# 方式1：直接运行
java -jar techdaily.jar

# 方式2：后台运行
nohup java -jar techdaily.jar > techdaily.log 2>&1 &

# 方式3：指定配置文件
java -jar techdaily.jar --spring.config.location=application.yml
```

### 4. 访问系统
- **前端界面**: http://localhost:8080
- **管理后台**: http://localhost:8080/admin
- **API文档**: http://localhost:8080/swagger-ui.html

## 📊 采集器配置

### 当前采集器列表
| 序号 | 采集器 | 类型 | 特点 | 采集数量 |
|------|--------|------|------|----------|
| 1 | Hacker News | 技术新闻 | 权威社区讨论 | 动态 |
| 2 | Dev.to | 开发者文章 | 活跃社区 | 动态 |
| 3 | InfoQ | 企业技术 | 专业深度分析 | 动态 |
| 4 | Daily.dev | 精选聚合 | AI驱动内容 | 动态 |
| 5 | **GitHub Trending** | 热门项目 | 🔥 **Top 3 项目** | **固定3个** |
| 6 | **IBM Developer** | 企业解决方案 | 💼 **Top 3 文章** | **固定3个** |

### 🔥 GitHub Trending 特性
- **实时热度**：追踪每日最热门的开源项目
- **星标统计**：显示项目星标数和今日增长
- **语言识别**：自动识别项目主要编程语言
- **项目描述**：提供详细的项目介绍

### 💼 IBM Developer 特性
- **企业级内容**：权威的企业技术解决方案
- **多领域覆盖**：AI/Watson、云计算、数据科学
- **技术深度**：深入的技术文章和教程
- **实用性强**：面向实际应用的技术指导

## ⚙️ 系统配置

### 采集频率设置
```yaml
# 定时任务配置
scheduler:
  crawl:
    cron: "0 0 8,14,20 * * ?" # 每天8点、14点、20点执行
  report:
    cron: "0 30 8 * * ?" # 每天8:30生成日报
```

### 采集器延迟配置
- **GitHub**: 2-4秒随机延迟（相对宽松）
- **IBM Developer**: 3-5秒随机延迟（企业级稳定）
- **其他源**: 3-6秒随机延迟（标准配置）

### AI服务配置
```yaml
ai:
  qwen:
    api-key: ${QWEN_API_KEY:your_api_key}
    model: qwen-turbo
    max-tokens: 2000
    temperature: 0.7
```

## 🔧 运维管理

### 日志监控
```bash
# 查看实时日志
tail -f techdaily.log

# 查看采集器日志
grep "Crawler" techdaily.log

# 查看错误日志
grep "ERROR" techdaily.log
```

### 健康检查
```bash
# 检查应用状态
curl http://localhost:8080/actuator/health

# 检查采集器状态
curl http://localhost:8080/api/crawlers/status
```

### 数据库维护
```sql
-- 查看文章统计
SELECT source, COUNT(*) as count FROM articles GROUP BY source;

-- 查看最新采集的文章
SELECT title, source, created_at FROM articles ORDER BY created_at DESC LIMIT 10;

-- 清理旧数据（保留30天）
DELETE FROM articles WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

## 🚨 故障排除

### 常见问题

1. **采集失败**
   - 检查网络连接
   - 验证目标网站可访问性
   - 查看采集器日志

2. **AI生成失败**
   - 检查API密钥配置
   - 验证网络连接
   - 查看AI服务日志

3. **数据库连接失败**
   - 检查数据库服务状态
   - 验证连接配置
   - 检查用户权限

### 性能优化

1. **采集优化**
   - 调整采集延迟参数
   - 优化采集器并发数
   - 启用采集缓存

2. **数据库优化**
   - 添加适当索引
   - 定期清理旧数据
   - 优化查询语句

## 📈 监控指标

### 关键指标
- **采集成功率**: > 90%
- **AI生成成功率**: > 95%
- **系统响应时间**: < 2秒
- **日报生成时间**: < 30秒

### 告警设置
- 采集失败率 > 20%
- AI服务异常
- 数据库连接异常
- 磁盘空间不足

## 🔄 版本升级

### 升级步骤
1. 备份数据库
2. 停止应用服务
3. 替换jar包
4. 更新配置文件
5. 启动新版本
6. 验证功能正常

### 回滚方案
1. 停止新版本
2. 恢复旧版本jar包
3. 恢复配置文件
4. 启动旧版本
5. 验证数据完整性

---

**版本**: v1.1.0  
**更新时间**: 2024-12-20  
**维护者**: TechDaily Team  
**支持**: 如有问题请查看日志或联系技术支持 