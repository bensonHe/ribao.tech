# TechDaily 技术日报系统 - 部署包

## 📦 版本信息

- **版本**: v1.0.0
- **编译时间**: 2025-05-29
- **Spring Boot**: 2.7.18
- **Java**: 8+

## 🚀 快速部署

### 1. 系统要求

- Java 8 或更高版本
- MySQL 5.7 或更高版本
- 至少 512MB 内存

### 2. 数据库准备

```bash
# 1. 登录MySQL
mysql -u root -p

# 2. 创建数据库
CREATE DATABASE techdaily CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 3. 创建用户（可选）
CREATE USER 'techdaily'@'%' IDENTIFIED BY 'TechDaily2025!';
GRANT ALL PRIVILEGES ON techdaily.* TO 'techdaily'@'%';
FLUSH PRIVILEGES;

# 4. 导入初始数据（可选）
mysql -u techdaily -p techdaily < init.sql
```

### 3. 配置文件

#### 生产环境配置 (application-prod.yml)
- 数据库连接配置
- 日志配置
- AI服务配置

#### 开发环境配置 (application-dev.yml)
- 本地开发数据库配置
- 调试日志配置

### 4. 启动应用

#### 方式一：使用启动脚本（推荐）
```bash
chmod +x start.sh
./start.sh
```

#### 方式二：直接运行jar包
```bash
# 使用生产配置
java -jar techdaily.jar --spring.profiles.active=prod

# 使用开发配置
java -jar techdaily.jar --spring.profiles.active=dev

# 指定配置文件
java -jar techdaily.jar --spring.config.location=application-prod.yml
```

#### 方式三：后台运行
```bash
nohup java -jar techdaily.jar --spring.profiles.active=prod > app.log 2>&1 &
```

### 5. 访问应用

- **首页**: http://localhost:8080
- **管理后台**: http://localhost:8080/spideAdmin/login
- **API文档**: http://localhost:8080/swagger-ui.html（如果启用）

### 6. 默认管理员账号

- **用户名**: admin
- **密码**: admin123（首次登录后请及时修改）

## 🔧 配置说明

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/techdaily?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: techdaily
    password: TechDaily2025!
```

### AI服务配置
```yaml
alibaba:
  ai:
    api-key: your-api-key-here
    model:
      translation: qwen-turbo
      summarization: qwen-plus
```

### 爬虫配置
```yaml
crawler:
  schedule:
    enabled: true
  sources:
    hackernews:
      enabled: true
    github:
      enabled: true
    devto:
      enabled: true
```

## 📋 功能特性

### ✅ 已实现功能

1. **文章爬取系统**
   - 支持 Hacker News、GitHub Trending、Dev.to
   - 自动去重和内容过滤
   - 定时爬取任务

2. **AI 日报生成**
   - 基于阿里云百炼大模型
   - 智能内容总结和分类
   - 每日趋势分析

3. **用户管理系统**
   - 用户注册登录
   - 角色权限管理
   - 密码加密存储

4. **管理后台**
   - 文章管理
   - 日报管理
   - 爬虫管理
   - 用户管理
   - 访问统计

5. **前端展示**
   - 响应式设计
   - 文章列表和详情
   - 日报查看
   - 搜索功能

### 🚧 待完善功能

1. **访问统计系统**
   - 页面访问量统计
   - 用户行为分析
   - 数据可视化图表

2. **缓存优化**
   - Redis 缓存集成
   - 热点数据缓存

3. **API 接口**
   - RESTful API
   - Swagger 文档

## 🐛 故障排查

### 常见问题

1. **端口被占用**
   ```bash
   # 查看端口占用
   lsof -i :8080
   # 杀死进程
   kill -9 PID
   ```

2. **数据库连接失败**
   - 检查数据库服务是否启动
   - 检查用户名密码是否正确
   - 检查防火墙设置

3. **内存不足**
   ```bash
   # 调整JVM内存参数
   java -Xms256m -Xmx512m -jar techdaily.jar
   ```

4. **日志查看**
   ```bash
   # 查看实时日志
   tail -f app.log
   
   # 查看错误日志
   grep ERROR app.log
   ```

## 📝 日志配置

日志文件位置：
- 应用日志：`app.log`
- 错误日志：`error.log`
- 访问日志：`access.log`

日志级别：
- INFO：一般信息
- WARN：警告信息
- ERROR：错误信息
- DEBUG：调试信息（开发环境）

## 🔐 安全建议

1. **修改默认密码**
   - 首次部署后立即修改管理员密码
   - 定期更新密码

2. **数据库安全**
   - 使用专用数据库用户
   - 限制数据库访问权限
   - 定期备份数据

3. **网络安全**
   - 配置防火墙规则
   - 使用HTTPS（生产环境）
   - 限制管理后台访问IP

4. **API密钥管理**
   - 妥善保管AI服务API密钥
   - 定期轮换密钥

## 📞 技术支持

如有问题，请联系：
- 邮箱：support@techdaily.com
- 文档：https://docs.techdaily.com
- Issues：https://github.com/techdaily/techdaily/issues

## 📄 许可证

MIT License - 详见 LICENSE 文件

---

**最后更新**: 2025-05-29
**维护者**: TechDaily Team 