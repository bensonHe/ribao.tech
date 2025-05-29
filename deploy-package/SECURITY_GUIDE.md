# 🔐 TechDaily 安全部署指南

本指南将帮助你安全地部署 TechDaily 应用，避免敏感信息泄露。

## 🚨 安全威胁说明

**为什么需要安全配置？**
- 🔑 数据库密码泄露可能导致数据被窃取或篡改
- 🤖 AI API密钥泄露可能产生巨额费用
- 🌐 服务器信息泄露可能遭受攻击
- 📂 敏感配置被提交到Git仓库后难以彻底删除

## ✅ 安全部署步骤

### 1. 环境变量配置

```bash
# 1. 复制环境变量模板
cp env.example .env

# 2. 编辑环境变量文件
vim .env
```

**配置示例：**
```bash
# 数据库配置
DB_HOST=your-db-host
DB_PORT=3306
DB_NAME=techdaily
DB_USERNAME=your-db-user
DB_PASSWORD=your-strong-password-here

# AI服务配置
ALIBABA_AI_API_KEY=sk-your-real-api-key-here

# 应用配置
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
```

### 2. 文件权限设置

```bash
# 设置环境变量文件权限（仅当前用户可读写）
chmod 600 .env

# 设置启动脚本权限
chmod +x start-secure.sh

# 确保敏感文件不会被误提交
git update-index --assume-unchanged .env
```

### 3. 使用安全启动脚本

```bash
# 检查配置
./start-secure.sh status

# 启动应用
./start-secure.sh start

# 查看日志
./start-secure.sh logs

# 停止应用
./start-secure.sh stop
```

## 🛡️ 安全最佳实践

### 密码安全
- ✅ 使用强密码（至少12位，包含大小写字母、数字、特殊字符）
- ✅ 定期更换密码
- ✅ 不在多个服务间复用密码
- ❌ 不使用简单或常见密码

### API密钥管理
- ✅ 定期轮换API密钥
- ✅ 设置API密钥使用限制
- ✅ 监控API密钥使用情况
- ❌ 不在日志中输出完整密钥

### 服务器安全
- ✅ 使用防火墙限制访问端口
- ✅ 定期更新系统和依赖
- ✅ 启用访问日志监控
- ✅ 使用HTTPS证书

### 代码仓库安全
- ✅ 永远不提交敏感信息到Git
- ✅ 使用.gitignore忽略敏感文件
- ✅ 定期审查提交历史
- ✅ 使用Git hooks防止误提交

## 🔍 安全检查清单

部署前请确认以下项目：

- [ ] `.env` 文件已正确配置且权限设置为 600
- [ ] 数据库密码足够强壮且唯一
- [ ] AI API密钥有效且已设置使用限制
- [ ] `.gitignore` 已包含所有敏感文件模式
- [ ] 敏感文件不在Git仓库中
- [ ] 防火墙已正确配置
- [ ] 应用日志不包含敏感信息
- [ ] 定期备份计划已制定

## 🚨 应急响应

### 如果密钥泄露
1. **立即更换**所有相关密码和API密钥
2. **审查日志**查看是否有异常访问
3. **通知相关方**密钥泄露事件
4. **清理Git历史**删除敏感信息

### 如果遭受攻击
1. **立即隔离**受影响的系统
2. **保存证据**收集攻击日志
3. **评估损失**检查数据完整性
4. **恢复服务**从安全备份恢复

## 📞 技术支持

如果遇到安全相关问题，请：
- 📧 通过 GitHub Issues 报告问题
- 🔒 对于严重安全漏洞，请私信联系
- 📚 查阅官方安全文档

---

**⚠️ 记住：安全是一个持续的过程，不是一次性的设置！** 