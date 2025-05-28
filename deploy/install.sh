#!/bin/bash

# TechDaily 服务器环境安装脚本
# 适用于 CentOS/RHEL 7+ 或 Ubuntu 18.04+

set -e

echo "🚀 开始安装 TechDaily 运行环境..."

# 检测操作系统
if [ -f /etc/redhat-release ]; then
    OS="centos"
    echo "检测到 CentOS/RHEL 系统"
elif [ -f /etc/lsb-release ]; then
    OS="ubuntu"
    echo "检测到 Ubuntu 系统"
else
    echo "❌ 不支持的操作系统"
    exit 1
fi

# 更新系统包
echo "📦 更新系统包..."
if [ "$OS" = "centos" ]; then
    yum update -y
    yum install -y wget curl vim unzip net-tools
elif [ "$OS" = "ubuntu" ]; then
    apt-get update -y
    apt-get install -y wget curl vim unzip net-tools
fi

# 安装 Java 8
echo "☕ 安装 Java 8..."
if [ "$OS" = "centos" ]; then
    yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel
elif [ "$OS" = "ubuntu" ]; then
    apt-get install -y openjdk-8-jdk
fi

# 验证 Java 安装
java -version
if [ $? -eq 0 ]; then
    echo "✅ Java 安装成功"
else
    echo "❌ Java 安装失败"
    exit 1
fi

# 安装 MySQL 8.0
echo "🗄️ 安装 MySQL 8.0..."
if [ "$OS" = "centos" ]; then
    # CentOS/RHEL - 使用官方仓库
    echo "正在添加 MySQL 官方仓库..."
    rpm -Uvh https://dev.mysql.com/get/mysql80-community-release-el7-3.noarch.rpm || true
    
    echo "正在安装 MySQL 服务器..."
    yum install -y mysql-community-server mysql-community-client
    
    # 启动 MySQL 服务
    echo "🔧 启动 MySQL 服务..."
    systemctl start mysqld
    systemctl enable mysqld
    
    # 等待 MySQL 启动
    echo "⏳ 等待 MySQL 启动..."
    sleep 10
    
    # 获取临时密码
    if [ -f /var/log/mysqld.log ]; then
        TEMP_PASSWORD=$(grep 'temporary password' /var/log/mysqld.log | tail -1 | awk '{print $NF}')
        if [ ! -z "$TEMP_PASSWORD" ]; then
            echo "📝 MySQL 临时密码: $TEMP_PASSWORD"
            echo "请记录此密码，稍后配置时需要使用"
        fi
    fi
    
elif [ "$OS" = "ubuntu" ]; then
    # Ubuntu - 使用官方仓库
    echo "正在下载 MySQL APT 配置包..."
    wget -q https://dev.mysql.com/get/mysql-apt-config_0.8.22-1_all.deb
    
    echo "正在配置 MySQL APT 仓库..."
    DEBIAN_FRONTEND=noninteractive dpkg -i mysql-apt-config_0.8.22-1_all.deb
    
    apt-get update
    
    echo "正在安装 MySQL 服务器..."
    # 预设置 root 密码以避免交互
    echo "mysql-server mysql-server/root_password password TechDaily2024!" | debconf-set-selections
    echo "mysql-server mysql-server/root_password_again password TechDaily2024!" | debconf-set-selections
    
    DEBIAN_FRONTEND=noninteractive apt-get install -y mysql-server mysql-client
    
    # 启动 MySQL 服务
    echo "🔧 启动 MySQL 服务..."
    systemctl start mysql
    systemctl enable mysql
    
    echo "📝 Ubuntu MySQL root 密码已设置为: TechDaily2024!"
fi

# 验证 MySQL 安装
echo "🔍 验证 MySQL 安装..."
sleep 5

# 检查 MySQL 服务状态
if systemctl is-active --quiet mysqld || systemctl is-active --quiet mysql; then
    echo "✅ MySQL 服务运行正常"
else
    echo "❌ MySQL 服务启动失败，尝试重新启动..."
    systemctl restart mysqld || systemctl restart mysql
    sleep 5
    
    if systemctl is-active --quiet mysqld || systemctl is-active --quiet mysql; then
        echo "✅ MySQL 服务重启成功"
    else
        echo "❌ MySQL 服务仍然无法启动"
        echo "请检查系统日志: journalctl -u mysqld 或 journalctl -u mysql"
        exit 1
    fi
fi

# 检查 MySQL 端口
if netstat -tlnp | grep -q ":3306 "; then
    echo "✅ MySQL 端口 3306 正在监听"
else
    echo "⚠️  MySQL 端口 3306 未监听，可能需要等待更长时间"
fi

# 创建应用目录
echo "📁 创建应用目录..."
mkdir -p /opt/techdaily
mkdir -p /var/log/techdaily
mkdir -p /etc/techdaily

# 设置目录权限
chown -R $USER:$USER /opt/techdaily
chown -R $USER:$USER /var/log/techdaily
chmod 755 /opt/techdaily
chmod 755 /var/log/techdaily

# 创建 systemd 服务文件
echo "🔧 创建 systemd 服务..."
cat > /etc/systemd/system/techdaily.service << EOF
[Unit]
Description=TechDaily Application
After=network.target mysqld.service mysql.service
Wants=mysqld.service mysql.service

[Service]
Type=simple
User=$USER
WorkingDirectory=/opt/techdaily
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod /opt/techdaily/techdaily.jar
ExecStop=/bin/kill -15 \$MAINPID
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=techdaily

# JVM 参数
Environment="JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC"

[Install]
WantedBy=multi-user.target
EOF

# 重新加载 systemd
systemctl daemon-reload

# 配置防火墙
echo "🔥 配置防火墙..."
if [ "$OS" = "centos" ]; then
    if systemctl is-active --quiet firewalld; then
        firewall-cmd --permanent --add-port=8080/tcp
        firewall-cmd --reload
        echo "✅ 防火墙规则已添加"
    fi
elif [ "$OS" = "ubuntu" ]; then
    if command -v ufw >/dev/null 2>&1; then
        ufw allow 8080/tcp
        echo "✅ 防火墙规则已添加"
    fi
fi

echo ""
echo "✅ 环境安装完成！"
echo ""
echo "📊 安装状态检查:"
echo "Java 版本: $(java -version 2>&1 | head -1)"
echo "MySQL 服务: $(systemctl is-active mysqld mysql 2>/dev/null || echo '未知')"
echo "MySQL 端口: $(netstat -tlnp | grep :3306 | wc -l) 个监听"
echo ""
echo "📋 接下来的步骤："
echo "1. 配置 MySQL root 密码"
echo "2. 运行数据库初始化脚本"
echo "3. 上传并启动应用"
echo ""
echo "🔧 MySQL 配置命令:"
echo "mysql_secure_installation"
echo ""
echo "📝 建议设置 MySQL root 密码为: TechDaily2024!" 