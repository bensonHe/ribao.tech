# TechDaily 编译脚本使用指南

## 📦 编译脚本概览

本项目提供了两个编译脚本，满足不同的使用场景：

### 1. `./quick-build.sh` - 快速编译脚本
- **用途**：开发环境快速编译测试
- **功能**：编译前端+后端，更新部署包中的jar文件
- **特点**：快速、轻量，适合频繁编译测试
- **输出**：只更新 `deploy-package/techdaily.jar`

### 2. `./build-deploy.sh` - 完整编译部署脚本  
- **用途**：生产环境完整部署包构建
- **功能**：完整编译，生成所有部署文件和脚本
- **特点**：全面、完整，适合正式发布
- **输出**：完整的部署包，包含所有必要文件

## 🚀 快速使用

### 开发环境快速编译
```bash
# 快速编译前端+后端
./quick-build.sh

# 然后可以直接在 deploy-package 目录启动测试
cd deploy-package
./restart.sh
```

### 生产环境完整构建
```bash
# 生成完整部署包
./build-deploy.sh

# 部署包已准备就绪，可以传输到服务器
cd deploy-package
tar -czf techdaily-deploy.tar.gz *
```

## 📋 环境要求

### 必需工具
- **Java 8+**：后端编译运行环境
- **Maven 3.6+**：后端构建工具
- **Node.js 14+**：前端编译环境 (如果有前端项目)
- **npm**：前端包管理器

### 检查环境
```bash
# 检查Java版本
java -version

# 检查Maven版本
mvn -version

# 检查Node.js版本 (如果有前端)
node -version

# 检查npm版本 (如果有前端)
npm -version
```

## 🔧 脚本详细说明

### quick-build.sh 功能
1. **前端编译**：
   - 检查并安装依赖（如果需要）
   - 执行 `npm run build`
   - 复制构建结果到后端静态资源目录
   
2. **后端编译**：
   - 执行 `mvn clean compile package -DskipTests`
   - 复制jar文件到部署目录

### build-deploy.sh 功能
1. **环境检查**：验证必要工具是否安装
2. **前端编译**：完整前端构建流程
3. **后端编译**：完整后端构建流程
4. **文件复制**：复制所有必要的配置文件
5. **脚本生成**：生成启动、停止、重启脚本
6. **文档生成**：生成版本信息和部署文档

## 📁 输出结构

### quick-build.sh 输出
```
deploy-package/
├── techdaily.jar          # ← 更新的jar文件
└── ... (其他文件保持不变)
```

### build-deploy.sh 输出  
```
deploy-package/
├── techdaily.jar          # 应用程序
├── start.sh              # 启动脚本
├── stop.sh               # 停止脚本
├── restart.sh            # 重启脚本
├── application-prod.yml   # 生产配置
├── init.sql              # 数据库初始化
├── logback-spring.xml    # 日志配置
├── VERSION.txt           # 版本信息
├── DEPLOY_README.md      # 部署说明
├── env.example           # 环境变量模板
└── static/               # 静态资源
```

## ⚡ 最佳实践

### 开发期间
```bash
# 快速编译测试
./quick-build.sh

# 启动应用测试
cd deploy-package && ./restart.sh

# 查看日志
tail -f deploy-package/app.log
```

### 发布准备
```bash
# 完整构建
./build-deploy.sh

# 检查构建结果
ls -la deploy-package/

# 创建发布包
cd deploy-package
tar -czf ../techdaily-v$(date +%Y%m%d).tar.gz *
```

### 持续集成
```bash
# CI/CD 脚本中使用
#!/bin/bash
set -e

# 完整构建
./build-deploy.sh

# 运行测试
cd deploy-package
./start.sh
sleep 30

# 健康检查
curl -f http://localhost:8080/actuator/health

# 清理
./stop.sh
```

## 🔍 故障排查

### 编译失败
1. **检查环境**：确认 Java、Maven、Node.js 版本
2. **清理缓存**：删除 `backend/target` 和 `frontend/node_modules`
3. **网络问题**：检查 Maven 和 npm 仓库连接

### 前端编译失败
```bash
# 清理前端缓存
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run build
```

### 后端编译失败
```bash
# 清理Maven缓存
cd backend
mvn clean
mvn dependency:resolve
mvn compile
```

## 📝 自定义配置

### 修改JVM参数
编辑 `build-deploy.sh` 中的启动脚本生成部分：
```bash
JVM_OPTS="-Xmx2048m -Xms1024m"  # 调整内存配置
```

### 修改构建目标
编辑脚本中的目录变量：
```bash
DEPLOY_DIR="$PROJECT_ROOT/my-deploy"  # 自定义部署目录
```

## 🚀 快速上手

1. **首次使用**：
   ```bash
   # 克隆项目后，首次完整构建
   ./build-deploy.sh
   ```

2. **日常开发**：
   ```bash
   # 修改代码后快速编译
   ./quick-build.sh
   ```

3. **生产部署**：
   ```bash
   # 生成部署包
   ./build-deploy.sh
   
   # 传输到服务器
   scp -r deploy-package/ user@server:/path/to/deploy/
   ```

现在您可以根据需要选择合适的编译脚本进行开发和部署了！ 