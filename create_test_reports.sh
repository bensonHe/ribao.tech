#!/bin/bash

# TechDaily 日报测试数据生成脚本

BASE_URL="http://localhost:8080"
COOKIE_FILE="cookies.txt"

echo "🚀 开始生成测试日报数据..."

# 登录
echo "🔐 正在登录..."
curl -c $COOKIE_FILE -X POST \
  -d "username=admin&password=123456a" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  $BASE_URL/spideAdmin/login \
  -s -o /dev/null

if [ $? -eq 0 ]; then
    echo "✅ 登录成功"
else
    echo "❌ 登录失败"
    exit 1
fi

# 生成今天的日报
echo "📰 生成今天的日报..."
TODAY=$(date +%Y-%m-%d)
RESPONSE=$(curl -b $COOKIE_FILE -X POST \
  -d "date=$TODAY" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  $BASE_URL/spideAdmin/reports/generate \
  -s)

echo "今天日报生成响应: $RESPONSE"

# 生成昨天的日报
echo "📰 生成昨天的日报..."
YESTERDAY=$(date -d "yesterday" +%Y-%m-%d 2>/dev/null || date -v-1d +%Y-%m-%d)
RESPONSE=$(curl -b $COOKIE_FILE -X POST \
  -d "date=$YESTERDAY" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  $BASE_URL/spideAdmin/reports/generate \
  -s)

echo "昨天日报生成响应: $RESPONSE"

# 生成前天的日报
echo "📰 生成前天的日报..."
DAY_BEFORE=$(date -d "2 days ago" +%Y-%m-%d 2>/dev/null || date -v-2d +%Y-%m-%d)
RESPONSE=$(curl -b $COOKIE_FILE -X POST \
  -d "date=$DAY_BEFORE" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  $BASE_URL/spideAdmin/reports/generate \
  -s)

echo "前天日报生成响应: $RESPONSE"

# 检查日报管理页面
echo "🔍 检查日报管理页面..."
curl -b $COOKIE_FILE -s $BASE_URL/spideAdmin/reports | grep -q "暂无日报"
if [ $? -eq 0 ]; then
    echo "ℹ️  日报管理页面显示：暂无日报"
else
    echo "✅ 日报管理页面有数据"
fi

# 检查首页
echo "🏠 检查首页日报..."
curl -s $BASE_URL/ | grep -q "技术日报"
if [ $? -eq 0 ]; then
    echo "✅ 首页包含日报内容"
else
    echo "ℹ️  首页暂无日报内容"
fi

echo "🎉 测试完成！" 