# 🔧 IDE开发环境安全配置指南

## 🎯 问题说明

在IDE中开发时，我们需要配置数据库和AI服务的敏感信息，但又不能将这些信息提交到Git仓库。

## ✅ 解决方案

### 方案1：IDE运行配置（推荐）

#### IntelliJ IDEA / WebStorm
1. **打开运行配置**
   - 点击右上角的运行配置下拉框
   - 选择 "Edit Configurations..."

2. **配置环境变量**
   - 找到你的Spring Boot启动配置
   - 在 "Environment variables" 栏点击文件夹图标
   - 添加以下环境变量：
   ```
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=techdaily
   DB_USERNAME=techdaily
   DB_PASSWORD=your-local-password
   ALIBABA_AI_API_KEY=sk-your-api-key
   ```

3. **保存配置**
   - 点击 OK 保存
   - 这些配置只保存在本地，不会被Git跟踪

#### VS Code
1. **创建launch.json**
   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "type": "java",
         "name": "TechDaily",
         "request": "launch",
         "mainClass": "com.spideman.SpidemanApplication",
         "env": {
           "DB_HOST": "localhost",
           "DB_PORT": "3306",
           "DB_NAME": "techdaily", 
           "DB_USERNAME": "techdaily",
           "DB_PASSWORD": "your-local-password",
           "ALIBABA_AI_API_KEY": "sk-your-api-key"
         }
       }
     ]
   }
   ```

### 方案2：本地配置文件

#### 创建开发专用配置文件
```yaml
# application-local.yml (已在.gitignore中)
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/techdaily?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: techdaily
    password: your-local-password
    
alibaba:
  ai:
    api-key: sk-your-local-api-key
```

#### 在IDE中指定profile
- **IntelliJ IDEA**: 在VM options中添加 `-Dspring.profiles.active=local`
- **命令行**: `mvn spring-boot:run -Dspring.profiles.active=local`

### 方案3：环境变量文件 + IDE插件

#### 安装EnvFile插件（IntelliJ IDEA）
1. File → Settings → Plugins
2. 搜索并安装 "EnvFile" 插件
3. 在运行配置中启用 "Enable EnvFile"
4. 指向你的 `.env` 文件

## 🔒 安全最佳实践

### 1. 确保敏感文件不被提交
```bash
# 检查哪些文件会被Git跟踪
git status

# 如果意外添加了敏感文件，立即移除
git rm --cached application-local.yml
```

### 2. 验证.gitignore规则
```bash
# 测试文件是否被忽略
echo "test" > .env
git status  # 应该看不到.env文件

echo "test" > application-local.yml  
git status  # 应该看不到这个文件
```

### 3. 团队协作建议
- 📝 在README中说明本地开发配置方法
- 🚫 永远不要在团队聊天中分享真实密码
- 🔄 定期轮换开发环境的数据库密码
- 📋 提供示例配置但使用假密码

## 🛠️ 快速配置模板

### IntelliJ IDEA 环境变量配置
```
DB_HOST=localhost
DB_PORT=3306
DB_NAME=techdaily
DB_USERNAME=techdaily
DB_PASSWORD=DevPassword123!
ALIBABA_AI_API_KEY=sk-development-key-here
SPRING_PROFILES_ACTIVE=dev
```

### 本地开发数据库建议
```sql
-- 为开发环境创建独立的数据库和用户
CREATE DATABASE techdaily_dev;
CREATE USER 'techdaily_dev'@'localhost' IDENTIFIED BY 'DevPassword123!';
GRANT ALL PRIVILEGES ON techdaily_dev.* TO 'techdaily_dev'@'localhost';
```

## ⚡ 常见问题

### Q: IDE配置会被提交到Git吗？
A: 不会！IDE的运行配置保存在 `.idea/runConfigurations/` 目录，已被.gitignore忽略。

### Q: 如何在团队间共享开发配置？
A: 创建配置文档和模板，但不要共享真实密码。每个开发者使用自己的本地密码。

### Q: 本地开发可以使用H2内存数据库吗？
A: 可以！在application-local.yml中配置H2，避免依赖MySQL。

### Q: 如何快速切换不同环境？
A: 使用Spring Profiles，在IDE中设置不同的启动配置。

---

**🎯 推荐方案：使用IDE环境变量配置，这是最安全且便于管理的方式！** 