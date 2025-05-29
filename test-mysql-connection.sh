#!/bin/bash

echo "🔍 测试MySQL连接..."

# MySQL配置
MYSQL_HOST="47.237.80.97"
MYSQL_PORT="3306"
MYSQL_USER="techdaily"
MYSQL_PASSWORD="TechDaily2024!"
DATABASE_NAME="techdaily"

# 测试连接
if mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -h "$MYSQL_HOST" -P "$MYSQL_PORT" -e "SELECT 1;" 2>/dev/null; then
    echo "✅ MySQL 连接成功"
    
    # 检查数据库
    if mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -h "$MYSQL_HOST" -P "$MYSQL_PORT" -e "USE $DATABASE_NAME;" 2>/dev/null; then
        echo "✅ 数据库 $DATABASE_NAME 存在"
        
        # 检查表
        table_count=$(mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -h "$MYSQL_HOST" -P "$MYSQL_PORT" "$DATABASE_NAME" -e "SHOW TABLES;" 2>/dev/null | wc -l)
        echo "📊 数据库中有 $((table_count-1)) 个表"
        
        # 检查文章数量
        article_count=$(mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -h "$MYSQL_HOST" -P "$MYSQL_PORT" "$DATABASE_NAME" -e "SELECT COUNT(*) FROM articles;" 2>/dev/null | tail -1)
        echo "📄 文章数量: $article_count"
        
        # 检查日报数量
        report_count=$(mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -h "$MYSQL_HOST" -P "$MYSQL_PORT" "$DATABASE_NAME" -e "SELECT COUNT(*) FROM daily_reports;" 2>/dev/null | tail -1)
        echo "📰 日报数量: $report_count"
        
    else
        echo "❌ 数据库 $DATABASE_NAME 不存在"
    fi
else
    echo "❌ MySQL 连接失败"
    echo "请检查网络连接和数据库配置"
fi 