#!/bin/bash

# TechDaily 安全启动脚本
# 使用环境变量进行配置，避免硬编码敏感信息

APP_NAME="techdaily"
JAR_FILE="techdaily.jar"
PID_FILE="${APP_NAME}.pid"
LOG_FILE="${APP_NAME}.log"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查环境变量文件
if [ ! -f ".env" ]; then
    echo -e "${RED}错误: 未找到 .env 文件！${NC}"
    echo -e "${YELLOW}请复制 env.example 为 .env 并配置正确的环境变量${NC}"
    echo ""
    echo "cp env.example .env"
    echo "vim .env  # 编辑配置文件"
    exit 1
fi

# 加载环境变量
echo -e "${GREEN}正在加载环境变量...${NC}"
export $(cat .env | grep -v '^#' | xargs)

# 验证必要的环境变量
check_env_var() {
    if [ -z "${!1}" ]; then
        echo -e "${RED}错误: 环境变量 $1 未设置！${NC}"
        exit 1
    fi
}

echo -e "${GREEN}检查必要的环境变量...${NC}"
check_env_var "DB_HOST"
check_env_var "DB_USERNAME" 
check_env_var "DB_PASSWORD"
check_env_var "ALIBABA_AI_API_KEY"

# 函数定义
start() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat $PID_FILE)
        if ps -p $PID > /dev/null 2>&1; then
            echo -e "${YELLOW}应用已经在运行中 (PID: $PID)${NC}"
            return 1
        else
            echo -e "${YELLOW}删除过期的PID文件${NC}"
            rm -f $PID_FILE
        fi
    fi

    echo -e "${GREEN}启动 $APP_NAME...${NC}"
    echo -e "${GREEN}数据库: ${DB_HOST}:${DB_PORT}/${DB_NAME}${NC}"
    echo -e "${GREEN}用户: ${DB_USERNAME}${NC}"
    echo -e "${GREEN}AI服务: ${ALIBABA_AI_API_KEY:0:10}...${NC}"
    
    # 启动应用
    nohup java -jar \
        -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} \
        -Dserver.port=${SERVER_PORT:-8080} \
        -DDB_HOST=${DB_HOST} \
        -DDB_PORT=${DB_PORT} \
        -DDB_NAME=${DB_NAME} \
        -DDB_USERNAME=${DB_USERNAME} \
        -DDB_PASSWORD=${DB_PASSWORD} \
        -DALIBABA_AI_API_KEY=${ALIBABA_AI_API_KEY} \
        $JAR_FILE > $LOG_FILE 2>&1 &
    
    echo $! > $PID_FILE
    echo -e "${GREEN}$APP_NAME 启动成功！ (PID: $(cat $PID_FILE))${NC}"
    echo -e "${GREEN}日志文件: $LOG_FILE${NC}"
    echo -e "${GREEN}访问地址: http://localhost:${SERVER_PORT:-8080}${NC}"
}

stop() {
    if [ ! -f "$PID_FILE" ]; then
        echo -e "${YELLOW}PID文件不存在，应用可能未运行${NC}"
        return 1
    fi

    PID=$(cat $PID_FILE)
    if ps -p $PID > /dev/null 2>&1; then
        echo -e "${YELLOW}停止 $APP_NAME (PID: $PID)...${NC}"
        kill $PID
        
        # 等待进程结束
        for i in {1..30}; do
            if ! ps -p $PID > /dev/null 2>&1; then
                break
            fi
            sleep 1
        done
        
        if ps -p $PID > /dev/null 2>&1; then
            echo -e "${RED}强制杀死进程...${NC}"
            kill -9 $PID
        fi
        
        rm -f $PID_FILE
        echo -e "${GREEN}$APP_NAME 已停止${NC}"
    else
        echo -e "${YELLOW}进程不存在，清理PID文件${NC}"
        rm -f $PID_FILE
    fi
}

status() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat $PID_FILE)
        if ps -p $PID > /dev/null 2>&1; then
            echo -e "${GREEN}$APP_NAME 正在运行 (PID: $PID)${NC}"
            return 0
        else
            echo -e "${RED}$APP_NAME 未运行（但PID文件存在）${NC}"
            return 1
        fi
    else
        echo -e "${RED}$APP_NAME 未运行${NC}"
        return 1
    fi
}

restart() {
    echo -e "${YELLOW}重启 $APP_NAME...${NC}"
    stop
    sleep 2
    start
}

logs() {
    if [ -f "$LOG_FILE" ]; then
        tail -f $LOG_FILE
    else
        echo -e "${RED}日志文件不存在: $LOG_FILE${NC}"
    fi
}

# 主逻辑
case "$1" in
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
    *)
        echo "用法: $0 {start|stop|restart|status|logs}"
        echo ""
        echo "  start   - 启动应用"
        echo "  stop    - 停止应用" 
        echo "  restart - 重启应用"
        echo "  status  - 查看状态"
        echo "  logs    - 查看日志"
        echo ""
        echo -e "${YELLOW}注意: 首次使用请先配置 .env 文件${NC}"
        exit 1
        ;;
esac

exit 0 