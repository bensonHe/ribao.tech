#!/bin/bash

# TechDaily 启动脚本
# 版本: v1.0.0
# 最后更新: 2025-05-29

# 配置变量
APP_NAME="TechDaily"
JAR_FILE="techdaily.jar"
PID_FILE="techdaily.pid"
LOG_FILE="app.log"
PROFILES="prod"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查Java环境
check_java() {
    if ! command -v java &> /dev/null; then
        log_error "Java 未安装或未在PATH中"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    log_info "Java版本: $JAVA_VERSION"
}

# 检查JAR文件
check_jar() {
    if [ ! -f "$JAR_FILE" ]; then
        log_error "JAR文件不存在: $JAR_FILE"
        exit 1
    fi
    log_info "JAR文件: $JAR_FILE ($(du -h $JAR_FILE | cut -f1))"
}

# 获取进程ID
get_pid() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            echo $PID
        else
            rm -f "$PID_FILE"
            echo ""
        fi
    else
        echo ""
    fi
}

# 启动应用
start() {
    log_info "正在启动 $APP_NAME..."
    
    # 检查是否已运行
    PID=$(get_pid)
    if [ ! -z "$PID" ]; then
        log_warn "应用已在运行，PID: $PID"
        return 1
    fi
    
    # 检查环境
    check_java
    check_jar
    
    # 启动应用
    log_info "使用配置文件: application-$PROFILES.yml"
    nohup java -Xms256m -Xmx512m \
        -Dfile.encoding=UTF-8 \
        -Duser.timezone=Asia/Shanghai \
        -jar "$JAR_FILE" \
        --spring.profiles.active="$PROFILES" \
        > "$LOG_FILE" 2>&1 &
    
    # 保存PID
    echo $! > "$PID_FILE"
    NEW_PID=$(cat "$PID_FILE")
    
    # 等待启动
    log_info "等待应用启动..."
    sleep 5
    
    # 检查是否启动成功
    if ps -p $NEW_PID > /dev/null 2>&1; then
        log_info "✅ $APP_NAME 启动成功！"
        log_info "PID: $NEW_PID"
        log_info "日志文件: $LOG_FILE"
        log_info "访问地址: http://localhost:8080"
        log_info "管理后台: http://localhost:8080/spideAdmin/login"
        
        # 检查端口监听
        sleep 3
        if netstat -ln 2>/dev/null | grep ":8080 " > /dev/null; then
            log_info "✅ 端口 8080 监听正常"
        else
            log_warn "⚠️ 端口 8080 未监听，请检查日志"
        fi
    else
        log_error "❌ $APP_NAME 启动失败"
        rm -f "$PID_FILE"
        exit 1
    fi
}

# 停止应用
stop() {
    log_info "正在停止 $APP_NAME..."
    
    PID=$(get_pid)
    if [ -z "$PID" ]; then
        log_warn "应用未运行"
        return 1
    fi
    
    # 优雅停止
    log_info "发送停止信号到进程 $PID"
    kill $PID
    
    # 等待停止
    for i in {1..30}; do
        if ! ps -p $PID > /dev/null 2>&1; then
            log_info "✅ $APP_NAME 已停止"
            rm -f "$PID_FILE"
            return 0
        fi
        sleep 1
    done
    
    # 强制停止
    log_warn "优雅停止失败，强制终止进程"
    kill -9 $PID
    rm -f "$PID_FILE"
    log_info "✅ $APP_NAME 已强制停止"
}

# 重启应用
restart() {
    log_info "正在重启 $APP_NAME..."
    stop
    sleep 2
    start
}

# 查看状态
status() {
    PID=$(get_pid)
    if [ ! -z "$PID" ]; then
        log_info "✅ $APP_NAME 正在运行"
        log_info "PID: $PID"
        log_info "内存使用: $(ps -o pid,vsz,rss,pmem,pcpu,time,comm -p $PID | tail -1)"
        
        # 检查端口
        if netstat -ln 2>/dev/null | grep ":8080 " > /dev/null; then
            log_info "✅ 端口 8080 监听正常"
        else
            log_warn "⚠️ 端口 8080 未监听"
        fi
        
        # 检查应用响应
        if command -v curl &> /dev/null; then
            if curl -s http://localhost:8080 > /dev/null; then
                log_info "✅ 应用响应正常"
            else
                log_warn "⚠️ 应用无响应"
            fi
        fi
    else
        log_warn "❌ $APP_NAME 未运行"
    fi
}

# 查看日志
logs() {
    if [ -f "$LOG_FILE" ]; then
        log_info "显示最新日志 (Ctrl+C 退出):"
        tail -f "$LOG_FILE"
    else
        log_warn "日志文件不存在: $LOG_FILE"
    fi
}

# 显示帮助
help() {
    echo -e "${BLUE}$APP_NAME 启动脚本${NC}"
    echo ""
    echo "用法: $0 {start|stop|restart|status|logs|help} [options]"
    echo ""
    echo "命令:"
    echo "  start    启动应用"
    echo "  stop     停止应用"
    echo "  restart  重启应用"
    echo "  status   查看状态"
    echo "  logs     查看日志"
    echo "  help     显示帮助"
    echo ""
    echo "选项:"
    echo "  --dev    使用开发环境配置"
    echo "  --prod   使用生产环境配置 (默认)"
    echo ""
    echo "示例:"
    echo "  $0 start"
    echo "  $0 start --dev"
    echo "  $0 restart --prod"
    echo ""
    echo "文件:"
    echo "  JAR文件: $JAR_FILE"
    echo "  PID文件: $PID_FILE"
    echo "  日志文件: $LOG_FILE"
    echo ""
    echo "访问地址:"
    echo "  首页: http://localhost:8080"
    echo "  管理后台: http://localhost:8080/spideAdmin/login"
}

# 解析参数
while [[ $# -gt 0 ]]; do
    case $1 in
        --dev)
            PROFILES="dev"
            log_info "使用开发环境配置"
            shift
            ;;
        --prod)
            PROFILES="prod"
            log_info "使用生产环境配置"
            shift
            ;;
        start|stop|restart|status|logs|help)
            COMMAND=$1
            shift
            ;;
        *)
            log_error "未知参数: $1"
            help
            exit 1
            ;;
    esac
done

# 默认命令
if [ -z "$COMMAND" ]; then
    COMMAND="help"
fi

# 执行命令
case $COMMAND in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    logs)
        logs
        ;;
    help)
        help
        ;;
    *)
        log_error "未知命令: $COMMAND"
        help
        exit 1
        ;;
esac 