#!/bin/bash

# TechDaily 应用启动脚本

APP_NAME="techdaily"
APP_DIR="/opt/techdaily"
JAR_FILE="$APP_DIR/techdaily.jar"
PID_FILE="$APP_DIR/techdaily.pid"
LOG_FILE="/var/log/techdaily/application.log"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# 启动应用
start_app() {
    print_message $BLUE "🚀 启动 TechDaily 应用..."
    
    if is_running; then
        print_message $YELLOW "⚠️  应用已经在运行中"
        return 1
    fi
    
    # 检查JAR文件是否存在
    if [ ! -f "$JAR_FILE" ]; then
        print_message $RED "❌ JAR文件不存在: $JAR_FILE"
        return 1
    fi
    
    # 检查Java是否安装
    if ! command -v java &> /dev/null; then
        print_message $RED "❌ Java 未安装"
        return 1
    fi
    
    # 检查MySQL是否运行
    if ! systemctl is-active --quiet mysqld && ! systemctl is-active --quiet mysql; then
        print_message $RED "❌ MySQL 服务未运行"
        print_message $YELLOW "尝试启动 MySQL..."
        systemctl start mysqld || systemctl start mysql
        sleep 5
        
        if ! systemctl is-active --quiet mysqld && ! systemctl is-active --quiet mysql; then
            print_message $RED "❌ MySQL 启动失败"
            return 1
        fi
        print_message $GREEN "✅ MySQL 启动成功"
    fi
    
    # 创建日志目录
    mkdir -p "$(dirname "$LOG_FILE")"
    
    # 启动应用
    print_message $BLUE "📦 启动 Java 应用..."
    cd "$APP_DIR"
    
    nohup java -jar \
        -Dspring.profiles.active=prod \
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
        # 检查端口是否监听
        if netstat -tlnp | grep -q ":8080 "; then
            print_message $GREEN "✅ TechDaily 启动成功！"
            print_message $GREEN "🌐 访问地址:"
            print_message $GREEN "   - 前端门户: http://$(hostname -I | awk '{print $1}'):8080/"
            print_message $GREEN "   - 管理后台: http://$(hostname -I | awk '{print $1}'):8080/spideAdmin/login"
            print_message $GREEN "   - 默认账户: admin / 111111"
            return 0
        else
            print_message $YELLOW "⚠️  应用已启动但端口未监听，可能需要更多时间"
            return 0
        fi
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
        return 1
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
        
    else
        print_message $RED "❌ 应用未运行"
    fi
    
    # 检查MySQL状态
    if systemctl is-active --quiet mysqld || systemctl is-active --quiet mysql; then
        print_message $GREEN "✅ MySQL 服务正常"
    else
        print_message $RED "❌ MySQL 服务未运行"
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
    echo "TechDaily 应用管理脚本"
    echo ""
    echo "用法: $0 {start|stop|restart|status|logs|help}"
    echo ""
    echo "命令:"
    echo "  start   - 启动应用"
    echo "  stop    - 停止应用"
    echo "  restart - 重启应用"
    echo "  status  - 查看状态"
    echo "  logs    - 查看日志"
    echo "  help    - 显示帮助"
    echo ""
    echo "示例:"
    echo "  $0 start    # 启动应用"
    echo "  $0 status   # 查看状态"
    echo "  $0 logs     # 查看日志"
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
    print_message $YELLOW "⚠️  建议不要使用 root 用户运行应用"
fi

# 执行主函数
main "$@" 