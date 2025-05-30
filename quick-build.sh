#!/bin/bash

# ===========================================
# TechDaily 快速编译脚本 (开发环境)
# ===========================================

set -e

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

echo ""
echo "⚡ TechDaily 快速编译脚本"
echo "========================="

PROJECT_ROOT=$(pwd)
FRONTEND_DIR="$PROJECT_ROOT/frontend"
BACKEND_DIR="$PROJECT_ROOT/backend"
DEPLOY_DIR="$PROJECT_ROOT/deploy-package"

# 编译前端 (如果存在)
if [ -d "$FRONTEND_DIR" ]; then
    log_info "快速编译前端..."
    cd "$FRONTEND_DIR"
    
    # 检查是否有 node_modules，如果没有则快速安装
    if [ ! -d "node_modules" ]; then
        log_info "安装前端依赖..."
        npm install --silent
    fi
    
    # 编译前端
    npm run build --silent
    
    # 检查构建结果
    if [ -d "dist" ] || [ -d "build" ]; then
        FRONTEND_BUILD_DIR="dist"
        if [ -d "build" ]; then
            FRONTEND_BUILD_DIR="build"
        fi
        
        # 复制到后端静态资源目录
        BACKEND_STATIC_DIR="$BACKEND_DIR/src/main/resources/static"
        if [ ! -d "$BACKEND_STATIC_DIR" ]; then
            mkdir -p "$BACKEND_STATIC_DIR"
        fi
        
        # 清理旧的静态资源
        rm -rf "$BACKEND_STATIC_DIR"/*
        
        # 复制新的前端构建结果
        cp -r "$FRONTEND_BUILD_DIR"/* "$BACKEND_STATIC_DIR/"
        
        log_success "前端编译完成"
    else
        log_warning "前端构建目录不存在"
    fi
    
    cd "$PROJECT_ROOT"
else
    log_warning "前端目录不存在，跳过前端编译"
fi

# 快速编译后端
log_info "快速编译后端..."
cd "$BACKEND_DIR"

# 只编译，不运行测试，静默模式
mvn clean compile package -DskipTests -q

# 查找jar文件
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" | head -1)
if [ -n "$JAR_FILE" ]; then
    # 复制到部署目录
    cp "$JAR_FILE" "$DEPLOY_DIR/techdaily.jar"
    log_success "后端编译完成！"
    echo "jar文件: $(ls -lh "$DEPLOY_DIR/techdaily.jar" | awk '{print $5}')"
else
    echo "编译失败！"
    exit 1
fi

cd "$PROJECT_ROOT"

echo ""
log_success "🎉 快速编译完成！前端+后端都已更新" 