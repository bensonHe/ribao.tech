# TechDaily 部署指南

## 📦 部署包内容

```
deploy-package/
├── techdaily.jar              # 应用JAR包 (53MB)
├── application-prod.yml       # 生产环境配置
├── init.sql                   # 数据库初始化脚本
├── logback-spring.xml         # 日志配置文件
├── start.sh                   # 启动脚本
└── README.md                  # 部署指南
```

## 📋 前提条件

- ✅ MySQL 已安装并配置
- ✅ MySQL 用户名: `root`
- ✅ MySQL 密码: `TechDaily2024!`
- ✅ Java 8+ 已安装
- ✅ 部署目录: `/usr/local/runtime/techdaily`

## 🚀 部署步骤

### 1. 上传文件到服务器

```bash
# 上传部署包到服务器
scp -r deploy-package/ root@47.237.80.97:/tmp/
```

### 2. 连接服务器并部署

```bash
# 连接服务器
ssh root@47.237.80.97

# 创建应用目录
mkdir -p /usr/local/runtime/techdaily

# 复制文件
cd /tmp/deploy-package
cp techdaily.jar /usr/local/runtime/techdaily/
cp start.sh /usr/local/runtime/techdaily/
cp init.sql /usr/local/runtime/techdaily/
cp logback-spring.xml /usr/local/runtime/techdaily/
chmod +x /usr/local/runtime/techdaily/start.sh

# 启动应用
cd /usr/local/runtime/techdaily
./start.sh start
```

## 🔧 启动脚本功能

### 主要功能

1. **数据库初始化**
   - 检查 MySQL 连接
   - 检查数据库 `techdaily` 是否存在，不存在则创建
   - 运行 `init.sql` 表初始化脚本

2. **Java程序启动**
   - 检查 JAR 文件和 Java 环境
   - 启动应用并记录进程 PID
   - 检查启动状态和端口监听

3. **重启功能**
   - 优雅停止现有进程
   - 重新启动应用

4. **增强的日志记录**
   - 详细记录文章查询过程和结果
   - AI日报生成的完整调用链路日志
   - 包含调用参数、耗时、错误信息等调试信息

### 使用命令

```bash
# 启动应用（包含数据库初始化）
./start.sh start

# 停止应用
./start.sh stop

# 重启应用
./start.sh restart

# 查看状态
./start.sh status

# 查看日志
./start.sh logs

# 仅初始化数据库
./start.sh init

# 显示帮助
./start.sh help
```

## 📁 文件位置

```
/usr/local/runtime/techdaily/
├── techdaily.jar              # 应用JAR包
├── start.sh                   # 启动脚本
├── init.sql                   # 数据库脚本
├── logback-spring.xml         # 日志配置文件
├── techdaily.pid              # 进程ID文件
└── logs/                      # 日志目录
    ├── app-info.log           # INFO级别日志
    ├── app-warn.log           # WARN级别日志
    ├── app-error.log          # ERROR级别日志
    ├── app2024-12-19-info.log # 按天轮换的历史日志
    ├── app2024-12-19-warn.log
    └── app2024-12-19-error.log

/var/log/techdaily/
└── application.log            # 启动日志
```

## 📋 日志配置

### 日志文件说明

- **app-info.log**: INFO级别日志，记录应用正常运行信息
- **app-warn.log**: WARN级别日志，记录警告信息
- **app-error.log**: ERROR级别日志，记录错误信息
- **按天轮换**: 每天自动创建新的日志文件，格式为 `app{yyyy-MM-dd}-{level}.log`

### 日志保留策略

- INFO日志: 保留30天，总大小限制10GB
- WARN日志: 保留30天，总大小限制5GB
- ERROR日志: 保留60天，总大小限制5GB

### 查看日志

```bash
# 查看实时INFO日志
tail -f /usr/local/runtime/techdaily/logs/app-info.log

# 查看实时ERROR日志
tail -f /usr/local/runtime/techdaily/logs/app-error.log

# 查看启动日志
tail -f /var/log/techdaily/application.log

# 查看所有日志文件
ls -la /usr/local/runtime/techdaily/logs/
```

## 🌐 访问地址

部署成功后，可通过以下地址访问：

- **前端门户**: http://47.237.80.97:8080/
- **管理后台**: http://47.237.80.97:8080/spideAdmin/login
- **默认账户**: admin / 111111

## 📊 状态检查

```bash
# 查看应用状态
./start.sh status

# 查看进程
ps aux | grep techdaily

# 查看端口
netstat -tlnp | grep 8080

# 查看日志
./start.sh logs
```

## 🔧 故障排除

### 1. 应用启动失败

```bash
# 查看详细日志
./start.sh logs

# 查看错误日志
tail -f /usr/local/runtime/techdaily/logs/app-error.log

# 检查Java版本
java -version

# 检查JAR文件
ls -la /usr/local/runtime/techdaily/techdaily.jar
```

### 2. 数据库连接失败

```bash
# 测试MySQL连接
mysql -u root -p"TechDaily2024!" -e "SELECT 1"

# 检查MySQL服务
systemctl status mysql

# 手动初始化数据库
./start.sh init
```

### 3. 日报生成问题（重要）

如果遇到"重新生成日报显示没查到最新文章"的问题：

```bash
# 查看详细的日报生成日志
tail -f /usr/local/runtime/techdaily/logs/app-info.log | grep -E "(🔍|📊|🤖|📋|📄|⚠️|✅|❌)"

# 检查今日文章查询
# 日志会显示：
# - 查询时间范围
# - 查询到的文章数量
# - 文章详细信息
# - AI调用参数和耗时

# 查看数据库中的文章
mysql -u root -p"TechDaily2024!" techdaily -e "
SELECT id, title, publish_time, status, source 
FROM articles 
WHERE publish_time >= CURDATE() 
ORDER BY publish_time DESC 
LIMIT 10;"

# 查看最近几天的文章
mysql -u root -p"TechDaily2024!" techdaily -e "
SELECT DATE(publish_time) as date, COUNT(*) as count 
FROM articles 
WHERE publish_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) 
GROUP BY DATE(publish_time) 
ORDER BY date DESC;"
```

**日志输出说明:**
- 🔍 表示开始查询文章
- 📊 表示查询统计信息
- 🤖 表示AI服务调用
- 📋 表示文章列表
- 📄 表示单篇文章信息
- ⚠️ 表示警告信息
- ✅ 表示成功操作
- ❌ 表示错误信息

### 4. 端口被占用

```bash
# 查看端口占用
netstat -tlnp | grep 8080
lsof -i :8080

# 杀死占用进程
kill -9 <PID>
```

### 5. 日志问题

```bash
# 检查日志目录权限
ls -la /usr/local/runtime/techdaily/logs/

# 检查磁盘空间
df -h

# 清理旧日志（如果需要）
find /usr/local/runtime/techdaily/logs/ -name "*.log" -mtime +30 -delete
```

## 🔄 更新应用

```bash
# 停止应用
./start.sh stop

# 备份当前版本
cp techdaily.jar techdaily.jar.backup

# 上传新版本JAR包
# 然后启动
./start.sh start
```

## 📝 配置说明

### MySQL 配置
- 用户名: `root`
- 密码: `TechDaily2024!`
- 数据库: `techdaily`

### JVM 参数
- 初始内存: 512MB
- 最大内存: 1024MB
- 垃圾收集器: G1GC
- 字符编码: UTF-8
- 时区: Asia/Shanghai

### 应用配置
- 运行环境: prod
- 端口: 8080
- 日志配置: logback-spring.xml

### 日志配置
- 日志目录: `/usr/local/runtime/techdaily/logs/`
- 异步日志: 提高性能
- 按级别分离: INFO、WARN、ERROR分别记录
- 自动轮换: 按天轮换，自动清理过期日志

---

**部署完成后，请及时修改默认密码并做好安全加固！** 