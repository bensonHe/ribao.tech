#!/bin/bash

# ===========================================
# TechDaily 自动编译部署脚本
# ===========================================

set -e  # 出错立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 命令未找到，请确保已安装"
        exit 1
    fi
}

# 开始编译
echo ""
echo "=========================================="
echo "    🚀 TechDaily 自动编译部署脚本"
echo "=========================================="
echo ""

BUILD_START_TIME=$(date +%s)

# 检查必要的命令
log_info "检查环境依赖..."
check_command "mvn"
check_command "java"

# 如果存在前端目录，检查前端依赖
if [ -d "frontend" ]; then
    check_command "npm"
fi

# 设置变量
PROJECT_ROOT=$(pwd)
BACKEND_DIR="$PROJECT_ROOT/backend"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
DEPLOY_DIR="$PROJECT_ROOT/deploy-package"
VERSION=$(date +"%Y%m%d_%H%M%S")
JAR_NAME="techdaily"

log_info "项目根目录: $PROJECT_ROOT"
log_info "编译版本: $VERSION"

# 创建或清理部署目录
log_info "准备部署目录..."
if [ ! -d "$DEPLOY_DIR" ]; then
    mkdir -p "$DEPLOY_DIR"
    log_info "创建部署目录: $DEPLOY_DIR"
else
    # 备份旧的jar文件
    if [ -f "$DEPLOY_DIR/${JAR_NAME}.jar" ]; then
        mv "$DEPLOY_DIR/${JAR_NAME}.jar" "$DEPLOY_DIR/${JAR_NAME}_backup_$(date +%Y%m%d_%H%M%S).jar"
        log_info "备份旧的jar文件"
    fi
fi

# ===========================================
# 编译前端 (如果存在)
# ===========================================
if [ -d "$FRONTEND_DIR" ]; then
    log_info "开始编译前端项目..."
    cd "$FRONTEND_DIR"
    
    # 安装依赖 (如果node_modules不存在)
    if [ ! -d "node_modules" ]; then
        log_info "安装前端依赖..."
        npm install
    fi
    
    # 编译前端
    log_info "构建前端项目..."
    npm run build
    
    # 检查build目录是否存在
    if [ -d "dist" ] || [ -d "build" ]; then
        # 复制前端构建结果到后端静态资源目录
        FRONTEND_BUILD_DIR="dist"
        if [ -d "build" ]; then
            FRONTEND_BUILD_DIR="build"
        fi
        
        BACKEND_STATIC_DIR="$BACKEND_DIR/src/main/resources/static"
        if [ ! -d "$BACKEND_STATIC_DIR" ]; then
            mkdir -p "$BACKEND_STATIC_DIR"
        fi
        
        log_info "复制前端构建结果到后端静态资源目录..."
        cp -r "$FRONTEND_BUILD_DIR"/* "$BACKEND_STATIC_DIR/"
        
        log_success "前端编译完成"
    else
        log_warning "前端构建目录不存在，跳过前端资源复制"
    fi
    
    cd "$PROJECT_ROOT"
else
    log_warning "前端目录不存在，跳过前端编译"
fi

# ===========================================
# 编译后端
# ===========================================
log_info "开始编译后端项目..."
cd "$BACKEND_DIR"

# 清理之前的构建
log_info "清理之前的构建..."
mvn clean

# 编译打包 (跳过测试以加快速度)
log_info "编译后端项目..."
mvn package -DskipTests

# 检查jar文件是否生成
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" | head -1)
if [ -z "$JAR_FILE" ]; then
    log_error "未找到编译生成的jar文件"
    exit 1
fi

log_success "后端编译完成: $JAR_FILE"

# ===========================================
# 复制文件到部署目录
# ===========================================
log_info "复制文件到部署目录..."

# 复制jar文件
cp "$JAR_FILE" "$DEPLOY_DIR/${JAR_NAME}.jar"
log_info "复制jar文件: ${JAR_NAME}.jar"

# 复制配置文件
if [ -f "$BACKEND_DIR/src/main/resources/application-prod.yml" ]; then
    cp "$BACKEND_DIR/src/main/resources/application-prod.yml" "$DEPLOY_DIR/"
    log_info "复制生产环境配置文件"
fi

# 复制数据库初始化脚本
if [ -f "$PROJECT_ROOT/deploy/init.sql" ]; then
    cp "$PROJECT_ROOT/deploy/init.sql" "$DEPLOY_DIR/"
    log_info "复制数据库初始化脚本"
fi

# 复制日志配置
if [ -f "$BACKEND_DIR/src/main/resources/logback-spring.xml" ]; then
    cp "$BACKEND_DIR/src/main/resources/logback-spring.xml" "$DEPLOY_DIR/"
    log_info "复制日志配置文件"
fi

# ===========================================
# 生成启动脚本
# ===========================================
log_info "生成启动脚本..."

cat > "$DEPLOY_DIR/start.sh" << 'EOF'
#!/bin/bash

# TechDaily 启动脚本

APP_NAME="techdaily"
JAR_FILE="${APP_NAME}.jar"
PID_FILE="${APP_NAME}.pid"
LOG_FILE="app.log"
JVM_OPTS="-Xmx1024m -Xms512m"
SPRING_PROFILES="prod"

# 检查是否已运行
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo "应用已在运行 (PID: $PID)"
        exit 1
    else
        rm -f "$PID_FILE"
    fi
fi

# 启动应用
echo "启动 TechDaily 应用..."
nohup java $JVM_OPTS -jar "$JAR_FILE" \
    --spring.profiles.active="$SPRING_PROFILES" \
    > "$LOG_FILE" 2>&1 &

# 保存PID
echo $! > "$PID_FILE"
echo "应用已启动 (PID: $!)"
echo "日志文件: $LOG_FILE"
echo "使用 'tail -f $LOG_FILE' 查看日志"
EOF

chmod +x "$DEPLOY_DIR/start.sh"

# ===========================================
# 生成停止脚本
# ===========================================
cat > "$DEPLOY_DIR/stop.sh" << 'EOF'
#!/bin/bash

# TechDaily 停止脚本

APP_NAME="techdaily"
PID_FILE="${APP_NAME}.pid"

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo "停止应用 (PID: $PID)..."
        kill $PID
        
        # 等待进程结束
        count=0
        while ps -p $PID > /dev/null 2>&1 && [ $count -lt 30 ]; do
            sleep 1
            count=$((count + 1))
        done
        
        if ps -p $PID > /dev/null 2>&1; then
            echo "强制停止应用..."
            kill -9 $PID
        fi
        
        rm -f "$PID_FILE"
        echo "应用已停止"
    else
        echo "应用未运行"
        rm -f "$PID_FILE"
    fi
else
    echo "PID文件不存在，应用可能未运行"
fi
EOF

chmod +x "$DEPLOY_DIR/stop.sh"

# ===========================================
# 生成重启脚本
# ===========================================
cat > "$DEPLOY_DIR/restart.sh" << 'EOF'
#!/bin/bash

# TechDaily 重启脚本

echo "重启 TechDaily 应用..."
./stop.sh
sleep 2
./start.sh
EOF

chmod +x "$DEPLOY_DIR/restart.sh"

# ===========================================
# 生成版本信息
# ===========================================
cat > "$DEPLOY_DIR/VERSION.txt" << EOF
TechDaily 构建信息
==================

构建时间: $(date)
构建版本: $VERSION
Git 提交: $(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
Git 分支: $(git branch --show-current 2>/dev/null || echo "unknown")
构建机器: $(hostname)
构建用户: $(whoami)

文件列表:
$(ls -la "$DEPLOY_DIR")

注意事项:
- 使用 ./start.sh 启动应用
- 使用 ./stop.sh 停止应用  
- 使用 ./restart.sh 重启应用
- 查看 app.log 获取运行日志
- 确保配置了正确的数据库连接信息
EOF

# ===========================================
# 生成部署文档
# ===========================================
cat > "$DEPLOY_DIR/DEPLOY_README.md" << 'EOF'
# TechDaily 部署包使用说明

## 📦 包含文件

- `techdaily.jar` - 主应用程序
- `start.sh` - 启动脚本
- `stop.sh` - 停止脚本  
- `restart.sh` - 重启脚本
- `application-prod.yml` - 生产环境配置
- `init.sql` - 数据库初始化脚本
- `logback-spring.xml` - 日志配置
- `VERSION.txt` - 版本信息

## 🚀 快速部署

### 1. 准备环境
```bash
# 确保安装了 Java 8+
java -version

# 确保安装了 MySQL 5.7+
mysql --version
```

### 2. 配置数据库
```bash
# 创建数据库
mysql -u root -p < init.sql
```

### 3. 配置应用
```bash
# 复制环境变量模板
cp env.example .env

# 编辑配置文件
vi .env
```

### 4. 启动应用
```bash
# 启动
./start.sh

# 查看日志
tail -f app.log

# 停止
./stop.sh

# 重启
./restart.sh
```

## 🔧 配置说明

主要配置文件是 `application-prod.yml`，需要配置：

- 数据库连接信息
- AI服务API密钥
- 服务器端口等

## 📊 监控

- 应用日志：`app.log`
- 进程PID：`techdaily.pid`
- 健康检查：`http://localhost:8080/actuator/health`

## 🆘 故障排查

1. 检查日志文件 `app.log`
2. 确认数据库连接正常
3. 检查端口是否被占用
4. 验证Java版本兼容性
EOF

# ===========================================
# 创建静态资源目录
# ===========================================
if [ ! -d "$DEPLOY_DIR/static" ]; then
    mkdir -p "$DEPLOY_DIR/static"
    log_info "创建静态资源目录"
fi

# 如果有额外的静态资源，复制过去
if [ -d "$BACKEND_DIR/src/main/resources/static" ]; then
    cp -r "$BACKEND_DIR/src/main/resources/static"/* "$DEPLOY_DIR/static/" 2>/dev/null || true
    log_info "复制静态资源文件"
fi

# ===========================================
# 完成编译
# ===========================================
cd "$PROJECT_ROOT"

BUILD_END_TIME=$(date +%s)
BUILD_DURATION=$((BUILD_END_TIME - BUILD_START_TIME))

log_success "编译部署包创建完成!"
echo ""
echo "=========================================="
echo "           📊 编译报告"
echo "=========================================="
echo "版本号: $VERSION"
echo "编译耗时: ${BUILD_DURATION}秒"
echo "部署目录: $DEPLOY_DIR"
echo "jar文件: ${JAR_NAME}.jar"
echo "jar大小: $(ls -lh "$DEPLOY_DIR/${JAR_NAME}.jar" | awk '{print $5}')"
echo ""
echo "📁 部署包内容："
ls -la "$DEPLOY_DIR"
echo ""
echo "🚀 部署命令："
echo "  cd $DEPLOY_DIR"
echo "  ./start.sh"
echo ""
echo "🔍 查看日志："
echo "  tail -f $DEPLOY_DIR/app.log"
echo ""
echo "=========================================="
log_success "构建完成！可以进行部署了。" 