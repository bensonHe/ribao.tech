# TechDaily - 每日技术精选门户

🚀 一个基于Spring Boot + React的技术文章聚合平台，通过AI智能生成每日技术日报。

## 📋 项目简介

TechDaily是一个现代化的技术文章聚合平台，主要功能包括：

- 🔍 **智能爬虫**: 自动抓取国外热门IT技术文章
- 🤖 **AI日报**: 基于阿里云百炼大模型生成每日技术日报
- 🌐 **双语支持**: 中英文双语显示，AI智能翻译
- 📱 **响应式设计**: 适配桌面和移动设备
- 🔐 **用户管理**: 完整的后台管理系统
- 📊 **数据统计**: 文章浏览量、用户统计等

## 🏗️ 技术架构

### 后端技术栈
- **框架**: Spring Boot 2.7.18
- **数据库**: MySQL 8.0 / H2 (开发环境)
- **ORM**: Spring Data JPA
- **安全**: Spring Security
- **AI服务**: 阿里云百炼大模型
- **爬虫**: Jsoup + RestTemplate

### 前端技术栈
- **框架**: React 18 + TypeScript
- **UI库**: Ant Design
- **构建工具**: Create React App
- **状态管理**: React Hooks

### 部署技术栈
- **容器化**: Docker (可选)
- **日志**: Logback + 按级别分离
- **监控**: 详细的性能日志和错误追踪

## 📦 项目结构

```
spideman/
├── backend/                    # Spring Boot后端
│   ├── src/main/java/
│   │   └── com/spideman/
│   │       ├── controller/     # 控制器层
│   │       ├── service/        # 服务层
│   │       ├── repository/     # 数据访问层
│   │       ├── entity/         # 实体类
│   │       ├── dto/           # 数据传输对象
│   │       └── config/        # 配置类
│   └── src/main/resources/
│       ├── templates/         # Thymeleaf模板
│       └── static/           # 静态资源
├── frontend/                  # React前端
│   ├── src/
│   │   ├── components/       # React组件
│   │   ├── pages/           # 页面组件
│   │   ├── services/        # API服务
│   │   └── types/          # TypeScript类型定义
│   └── public/             # 公共资源
├── deploy-package/           # 生产部署包
│   ├── techdaily.jar        # 应用JAR包
│   ├── start.sh            # 启动脚本
│   ├── init.sql           # 数据库初始化
│   ├── logback-spring.xml # 日志配置
│   └── README.md          # 部署指南
└── mock-backend/           # Node.js模拟后端
```

## 🚀 快速开始

### 开发环境要求

- Java 8+
- Node.js 16+
- MySQL 8.0 (生产环境)
- Maven 3.6+

### 本地开发

1. **克隆项目**
```bash
git clone git@github.com:bensonHe/spideman.git
cd spideman
```

2. **启动后端**
```bash
cd backend
mvn spring-boot:run
```

3. **启动前端**
```bash
cd frontend
npm install
npm start
```

4. **访问应用**
- 前端门户: http://localhost:3000
- 后端API: http://localhost:8080
- 管理后台: http://localhost:8080/spideAdmin/login

### 生产部署

详细部署指南请参考: [deploy-package/README.md](deploy-package/README.md)

```bash
# 快速部署到服务器
scp -r deploy-package/ root@your-server:/tmp/
ssh root@your-server
cd /tmp/deploy-package
./start.sh start
```

## 🔧 核心功能

### 1. 智能文章爬虫
- 支持多个技术网站的文章抓取
- 自动去重和内容清洗
- 智能标签分类

### 2. AI日报生成
- 基于当日文章智能生成技术日报
- 包含热门话题、技术趋势、关键洞察
- 支持中英文双语输出

### 3. 用户管理系统
- 管理员账户管理
- 用户权限控制
- 密码安全策略

### 4. 数据统计分析
- 文章浏览量统计
- 用户行为分析
- 系统性能监控

## 📊 系统监控

### 日志系统
- **INFO日志**: 记录正常业务流程
- **WARN日志**: 记录警告信息
- **ERROR日志**: 记录错误和异常
- **按天轮换**: 自动清理过期日志

### 性能监控
- 文章查询耗时监控
- AI调用性能分析
- 数据库查询优化

## 🔐 安全特性

- Spring Security安全框架
- BCrypt密码加密
- CSRF防护
- XSS防护
- SQL注入防护

## 🌟 特色功能

### 增强的日志调试
- 🔍 详细的文章查询日志
- 🤖 AI服务调用链路追踪
- 📊 性能指标实时监控
- ⚠️ 智能错误诊断和建议

### 智能降级机制
- 今日无文章时自动返回最近文章
- AI服务异常时的备选方案
- 数据库连接失败的容错处理

## 📝 API文档

### 主要接口

- `GET /api/articles` - 获取文章列表
- `POST /api/daily-report` - 生成AI日报
- `GET /api/articles/search` - 搜索文章
- `POST /spideAdmin/login` - 管理员登录

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 🙏 致谢

- [Spring Boot](https://spring.io/projects/spring-boot) - 后端框架
- [React](https://reactjs.org/) - 前端框架
- [Ant Design](https://ant.design/) - UI组件库
- [阿里云百炼](https://www.aliyun.com/product/bailian) - AI服务
- [Jsoup](https://jsoup.org/) - HTML解析库

## 📞 联系方式

- 项目地址: https://github.com/bensonHe/spideman
- 问题反馈: [Issues](https://github.com/bensonHe/spideman/issues)

---

**让技术学习更高效！** 🚀 