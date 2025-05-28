#!/bin/bash

# TechDaily 启动脚本
# 功能：数据库初始化、启动Java程序、重启功能

set -e

# 配置参数
APP_NAME="techdaily"
APP_DIR="/usr/local/runtime/techdaily"
JAR_FILE="$APP_DIR/techdaily.jar"
PID_FILE="$APP_DIR/techdaily.pid"
LOG_FILE="/var/log/techdaily/application.log"

# MySQL 配置
MYSQL_USER="root"
MYSQL_PASSWORD="TechDaily2024!"
DATABASE_NAME="techdaily"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 打印带颜色的消息
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# 检查应用是否运行
is_running() {
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            return 0
        else
            rm -f "$PID_FILE"
            return 1
        fi
    fi
    return 1
}

# 初始化数据库
init_database() {
    print_message $BLUE "🗄️ 初始化数据库..."
    
    # 检查MySQL连接
    if ! mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1;" 2>/dev/null; then
        print_message $RED "❌ MySQL 连接失败"
        print_message $YELLOW "请检查 MySQL 服务状态和密码配置"
        exit 1
    fi
    
    # 检查数据库是否存在
    if mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "USE $DATABASE_NAME;" 2>/dev/null; then
        print_message $GREEN "✅ 数据库 $DATABASE_NAME 已存在"
    else
        print_message $YELLOW "⚠️  数据库 $DATABASE_NAME 不存在，正在创建..."
        mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "CREATE DATABASE $DATABASE_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
        print_message $GREEN "✅ 数据库 $DATABASE_NAME 创建成功"
    fi
    
    # 运行表初始化脚本
    if [ -f "init.sql" ]; then
        print_message $BLUE "📊 运行表初始化脚本..."
        mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$DATABASE_NAME" < init.sql
        print_message $GREEN "✅ 表初始化完成"
    else
        print_message $YELLOW "⚠️  找不到 init.sql 文件，跳过表初始化"
    fi
}

# 启动应用
start_app() {
    print_message $BLUE "🚀 启动 TechDaily 应用..."
    
    if is_running; then
        print_message $YELLOW "⚠️  应用已经在运行中"
        local pid=$(cat "$PID_FILE")
        print_message $BLUE "当前进程 PID: $pid"
        return 0
    fi
    
    # 检查JAR文件是否存在
    if [ ! -f "$JAR_FILE" ]; then
        print_message $RED "❌ JAR文件不存在: $JAR_FILE"
        print_message $YELLOW "请确保 techdaily.jar 已复制到 $APP_DIR 目录"
        exit 1
    fi
    
    # 检查Java是否安装
    if ! command -v java &> /dev/null; then
        print_message $RED "❌ Java 未安装"
        exit 1
    fi
    
    # 初始化数据库
    init_database
    
    # 创建日志目录
    mkdir -p "$(dirname "$LOG_FILE")"
    mkdir -p "$APP_DIR/logs"
    
    # 启动应用
    print_message $BLUE "📦 启动 Java 应用..."
    cd "$APP_DIR"
    
    nohup java -jar \
        -Dspring.profiles.active=prod \
        -Dlogging.config=./logback-spring.xml \
        -Xms512m \
        -Xmx1024m \
        -XX:+UseG1GC \
        -Dfile.encoding=UTF-8 \
        -Duser.timezone=Asia/Shanghai \
        "$JAR_FILE" \
        > "$LOG_FILE" 2>&1 &
    
    local pid=$!
    echo $pid > "$PID_FILE"
    
    # 等待应用启动
    print_message $BLUE "⏳ 等待应用启动..."
    sleep 10
    
    # 检查应用是否启动成功
    if is_running; then
        print_message $GREEN "✅ 应用启动成功！"
        print_message $BLUE "进程 PID: $(cat "$PID_FILE")"
        
        # 检查端口是否监听
        sleep 5
        if netstat -tlnp | grep -q ":8080 "; then
            print_message $GREEN "✅ 端口 8080 正在监听"
            print_message $GREEN "🌐 访问地址:"
            print_message $GREEN "   - 前端门户: http://$(hostname -I | awk '{print $1}'):8080/"
            print_message $GREEN "   - 管理后台: http://$(hostname -I | awk '{print $1}'):8080/spideAdmin/login"
            print_message $GREEN "   - 默认账户: admin / 111111"
            print_message $BLUE "📋 日志文件:"
            print_message $BLUE "   - 应用日志: $APP_DIR/logs/"
            print_message $BLUE "   - 启动日志: $LOG_FILE"
        else
            print_message $YELLOW "⚠️  端口 8080 未监听，应用可能需要更多时间启动"
            print_message $BLUE "查看日志: tail -f $LOG_FILE"
        fi
        return 0
    else
        print_message $RED "❌ 应用启动失败"
        print_message $RED "查看日志: tail -f $LOG_FILE"
        return 1
    fi
}

# 停止应用
stop_app() {
    print_message $BLUE "🛑 停止 TechDaily 应用..."
    
    if ! is_running; then
        print_message $YELLOW "⚠️  应用未运行"
        return 0
    fi
    
    local pid=$(cat "$PID_FILE")
    print_message $BLUE "正在停止进程 $pid..."
    
    # 优雅停止
    kill -TERM "$pid"
    
    # 等待进程停止
    local count=0
    while [ $count -lt 30 ]; do
        if ! ps -p "$pid" > /dev/null 2>&1; then
            rm -f "$PID_FILE"
            print_message $GREEN "✅ 应用已停止"
            return 0
        fi
        sleep 1
        count=$((count + 1))
    done
    
    # 强制停止
    print_message $YELLOW "⚠️  优雅停止超时，强制停止..."
    kill -KILL "$pid" 2>/dev/null || true
    rm -f "$PID_FILE"
    print_message $GREEN "✅ 应用已强制停止"
}

# 重启应用
restart_app() {
    print_message $BLUE "🔄 重启 TechDaily 应用..."
    stop_app
    sleep 2
    start_app
}

# 查看应用状态
status_app() {
    print_message $BLUE "📊 TechDaily 应用状态:"
    
    if is_running; then
        local pid=$(cat "$PID_FILE")
        print_message $GREEN "✅ 应用正在运行 (PID: $pid)"
        
        # 检查端口
        if netstat -tlnp | grep -q ":8080 "; then
            print_message $GREEN "✅ 端口 8080 正在监听"
        else
            print_message $YELLOW "⚠️  端口 8080 未监听"
        fi
        
        # 显示内存使用
        local memory=$(ps -p "$pid" -o rss= 2>/dev/null | awk '{print int($1/1024)"MB"}')
        if [ ! -z "$memory" ]; then
            print_message $BLUE "📊 内存使用: $memory"
        fi
        
        # 显示运行时间
        local start_time=$(ps -p "$pid" -o lstart= 2>/dev/null)
        if [ ! -z "$start_time" ]; then
            print_message $BLUE "⏰ 启动时间: $start_time"
        fi
        
    else
        print_message $RED "❌ 应用未运行"
    fi
    
    # 检查MySQL状态
    if mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1;" 2>/dev/null; then
        print_message $GREEN "✅ MySQL 连接正常"
    else
        print_message $RED "❌ MySQL 连接失败"
    fi
    
    # 检查数据库
    if mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "USE $DATABASE_NAME;" 2>/dev/null; then
        print_message $GREEN "✅ 数据库 $DATABASE_NAME 存在"
    else
        print_message $RED "❌ 数据库 $DATABASE_NAME 不存在"
    fi
}

# 查看日志
logs_app() {
    if [ -f "$LOG_FILE" ]; then
        print_message $BLUE "📋 应用日志 (最后50行):"
        tail -n 50 "$LOG_FILE"
        print_message $BLUE "\n实时日志: tail -f $LOG_FILE"
    else
        print_message $YELLOW "⚠️  日志文件不存在: $LOG_FILE"
    fi
}

# 显示帮助信息
show_help() {
    echo "TechDaily 启动脚本"
    echo ""
    echo "用法: $0 {start|stop|restart|status|logs|init|help}"
    echo ""
    echo "命令:"
    echo "  start   - 初始化数据库并启动应用"
    echo "  stop    - 停止应用"
    echo "  restart - 重启应用"
    echo "  status  - 查看状态"
    echo "  logs    - 查看日志"
    echo "  init    - 仅初始化数据库"
    echo "  help    - 显示帮助"
    echo ""
    echo "配置信息:"
    echo "  - 应用目录: $APP_DIR"
    echo "  - JAR文件: $JAR_FILE"
    echo "  - 日志文件: $LOG_FILE"
    echo "  - 数据库: $DATABASE_NAME"
    echo "  - MySQL用户: $MYSQL_USER"
    echo ""
    echo "示例:"
    echo "  $0 start    # 启动应用"
    echo "  $0 status   # 查看状态"
    echo "  $0 restart  # 重启应用"
}

# 主函数
main() {
    case "$1" in
        start)
            start_app
            ;;
        stop)
            stop_app
            ;;
        restart)
            restart_app
            ;;
        status)
            status_app
            ;;
        logs)
            logs_app
            ;;
        init)
            init_database
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_message $RED "❌ 未知命令: $1"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# 检查是否为root用户
if [ "$EUID" -eq 0 ]; then
    print_message $YELLOW "⚠️  当前为 root 用户"
fi

# 执行主函数
main "$@" 