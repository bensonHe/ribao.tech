#!/bin/bash

# TechDaily 应用部署脚本

set -e

APP_NAME="techdaily"
APP_DIR="/opt/techdaily"
LOG_DIR="/var/log/techdaily"
CONFIG_DIR="/etc/techdaily"

echo "🚀 开始部署 TechDaily 应用..."

# 检查必要文件
if [ ! -f "techdaily.jar" ]; then
    echo "❌ 找不到 techdaily.jar 文件"
    exit 1
fi

if [ ! -f "application-prod.yml" ]; then
    echo "❌ 找不到 application-prod.yml 配置文件"
    exit 1
fi

if [ ! -f "init.sql" ]; then
    echo "❌ 找不到 init.sql 数据库初始化脚本"
    exit 1
fi

# 检查MySQL服务状态
echo "🔍 检查 MySQL 服务状态..."
if ! systemctl is-active --quiet mysqld && ! systemctl is-active --quiet mysql; then
    echo "❌ MySQL 服务未运行，尝试启动..."
    systemctl start mysqld || systemctl start mysql
    sleep 5
    
    if ! systemctl is-active --quiet mysqld && ! systemctl is-active --quiet mysql; then
        echo "❌ MySQL 服务启动失败"
        echo "请检查 MySQL 安装: systemctl status mysqld"
        exit 1
    fi
fi
echo "✅ MySQL 服务正常运行"

# 停止现有服务
echo "🛑 停止现有服务..."
systemctl stop techdaily || true

# 备份现有应用 (如果存在)
if [ -f "$APP_DIR/techdaily.jar" ]; then
    echo "📦 备份现有应用..."
    cp "$APP_DIR/techdaily.jar" "$APP_DIR/techdaily.jar.backup.$(date +%Y%m%d_%H%M%S)"
fi

# 复制新的应用文件
echo "📁 部署新应用文件..."
cp techdaily.jar "$APP_DIR/"
cp application-prod.yml "$CONFIG_DIR/"

# 设置文件权限
chown $USER:$USER "$APP_DIR/techdaily.jar"
chown $USER:$USER "$CONFIG_DIR/application-prod.yml"
chmod 755 "$APP_DIR/techdaily.jar"

# 测试MySQL连接和初始化数据库
echo "🗄️ 配置数据库..."

# 尝试不同的连接方式
MYSQL_CONNECTED=false
MYSQL_PASSWORD=""

# 首先尝试无密码连接（新安装的MySQL可能没有密码）
if mysql -u root -e "SELECT 1;" 2>/dev/null; then
    echo "✅ MySQL 无密码连接成功"
    MYSQL_CONNECTED=true
else
    # 尝试使用预设密码
    if mysql -u root -p"TechDaily2024!" -e "SELECT 1;" 2>/dev/null; then
        echo "✅ MySQL 使用预设密码连接成功"
        MYSQL_PASSWORD="TechDaily2024!"
        MYSQL_CONNECTED=true
    else
        # 需要用户输入密码
        echo "🔑 需要输入 MySQL root 密码"
        read -p "请输入 MySQL root 密码: " -s MYSQL_PASSWORD
        echo
        
        if mysql -u root -p"$MYSQL_PASSWORD" -e "SELECT 1;" 2>/dev/null; then
            echo "✅ MySQL 密码验证成功"
            MYSQL_CONNECTED=true
        else
            echo "❌ MySQL 密码错误"
            exit 1
        fi
    fi
fi

if [ "$MYSQL_CONNECTED" = false ]; then
    echo "❌ 无法连接到 MySQL"
    exit 1
fi

# 执行数据库初始化
echo "📊 初始化数据库..."
if [ -z "$MYSQL_PASSWORD" ]; then
    mysql -u root < init.sql
else
    mysql -u root -p"$MYSQL_PASSWORD" < init.sql
fi

if [ $? -eq 0 ]; then
    echo "✅ 数据库初始化成功"
else
    echo "❌ 数据库初始化失败"
    exit 1
fi

# 创建应用配置文件链接
ln -sf "$CONFIG_DIR/application-prod.yml" "$APP_DIR/application-prod.yml"

# 复制启动脚本
if [ -f "start-app.sh" ]; then
    echo "📋 复制启动脚本..."
    cp start-app.sh "$APP_DIR/"
    chmod +x "$APP_DIR/start-app.sh"
fi

# 启动服务
echo "🔧 启动 TechDaily 服务..."
systemctl start techdaily
systemctl enable techdaily

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 15

# 检查服务状态
if systemctl is-active --quiet techdaily; then
    echo "✅ TechDaily 服务启动成功！"
    
    # 等待端口监听
    echo "🔍 检查端口状态..."
    for i in {1..30}; do
        if netstat -tlnp | grep -q ":8080 "; then
            echo "✅ 端口 8080 正在监听"
            break
        fi
        sleep 2
        echo "等待端口监听... ($i/30)"
    done
    
    # 显示服务状态
    echo ""
    echo "📊 服务状态:"
    systemctl status techdaily --no-pager -l
    
    echo ""
    echo "🌐 访问地址:"
    SERVER_IP=$(hostname -I | awk '{print $1}')
    echo "  - 前端门户: http://$SERVER_IP:8080/"
    echo "  - 管理后台: http://$SERVER_IP:8080/spideAdmin/login"
    echo "  - 默认账户: admin / 111111"
    
    echo ""
    echo "📝 常用命令:"
    echo "  - 查看日志: journalctl -u techdaily -f"
    echo "  - 重启服务: systemctl restart techdaily"
    echo "  - 停止服务: systemctl stop techdaily"
    echo "  - 查看状态: systemctl status techdaily"
    
    if [ -f "$APP_DIR/start-app.sh" ]; then
        echo "  - 应用管理: $APP_DIR/start-app.sh {start|stop|restart|status|logs}"
    fi
    
else
    echo "❌ TechDaily 服务启动失败！"
    echo ""
    echo "📋 查看错误日志:"
    echo "journalctl -u techdaily -n 50"
    echo ""
    echo "🔧 可能的解决方案:"
    echo "1. 检查 MySQL 连接: mysql -u root -p"
    echo "2. 检查 Java 版本: java -version"
    echo "3. 检查端口占用: netstat -tlnp | grep 8080"
    echo "4. 检查应用日志: tail -f /var/log/techdaily/application.log"
    exit 1
fi

echo ""
echo "🎉 部署完成！" 