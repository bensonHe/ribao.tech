#!/bin/bash

echo "🚀 TechDaily - IT技术日报系统启动脚本"
echo "======================================"

# 检查Node.js是否安装
if ! command -v node &> /dev/null; then
    echo "❌ Node.js 未安装，请先安装 Node.js"
    exit 1
fi

# 检查npm是否安装
if ! command -v npm &> /dev/null; then
    echo "❌ npm 未安装，请先安装 npm"
    exit 1
fi

echo "✅ Node.js 和 npm 已安装"

# 检查是否有Java和Maven
HAS_JAVA=false
if command -v java &> /dev/null && command -v mvn &> /dev/null; then
    echo "✅ 检测到 Java 和 Maven，将启动 Spring Boot 后端"
    HAS_JAVA=true
else
    echo "ℹ️  未检测到 Java/Maven，将使用 Node.js 模拟后端"
fi

# 启动后端
if [ "$HAS_JAVA" = true ]; then
    echo "🚀 启动 Spring Boot 后端服务器..."
    cd backend
    export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_211.jdk/Contents/Home
    mvn spring-boot:run &
    BACKEND_PID=$!
    echo "✅ Spring Boot 后端已启动 (PID: $BACKEND_PID)"
    cd ..
else
    echo "🚀 启动模拟后端服务器..."
    cd mock-backend
    if [ ! -d "node_modules" ]; then
        echo "📦 安装后端依赖..."
        npm install
    fi
    npm start &
    BACKEND_PID=$!
    echo "✅ 模拟后端已启动 (PID: $BACKEND_PID)"
    cd ..
fi

# 等待后端启动
echo "⏳ 等待后端启动..."
sleep 10

# 启动前端
echo "🚀 启动前端开发服务器..."
cd frontend
if [ ! -d "node_modules" ]; then
    echo "📦 安装前端依赖..."
    npm install
fi

echo "✅ 前端服务器启动中..."
echo "📱 前端地址: http://localhost:3000"
echo "🔗 后端API: http://localhost:8080/api/articles"
echo ""
echo "🎉 TechDaily 技术门户网站已就绪！"
echo "按 Ctrl+C 停止所有服务"

# 前台启动前端
npm start

# 当前端停止时，也停止后端
echo "🛑 停止后端服务器..."
kill $BACKEND_PID 2>/dev/null 