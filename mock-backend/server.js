const express = require('express');
const cors = require('cors');

const app = express();
const PORT = 8080;

// 启用CORS
app.use(cors());
app.use(express.json());

// 模拟数据
const mockArticles = [
  {
    id: 1,
    title: "React 19 Beta: New Features and Improvements",
    titleZh: "React 19 Beta：新功能和改进",
    summary: "React 19 introduces new hooks, server components, and performance improvements.",
    summaryZh: "React 19 引入了新的 hooks、服务器组件和性能改进。",
    url: "https://react.dev/blog/2024/12/05/react-19-beta",
    source: "React.dev",
    author: "React Team",
    publishTime: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
    createdAt: new Date().toISOString(),
    likes: 23,
    views: 156,
    tags: "React,Frontend,JavaScript",
    status: "PUBLISHED"
  },
  {
    id: 2,
    title: "Spring Boot 3.2: What's New",
    titleZh: "Spring Boot 3.2：新特性介绍",
    summary: "Spring Boot 3.2 brings GraalVM native image support and virtual threads.",
    summaryZh: "Spring Boot 3.2 带来了 GraalVM 本地镜像支持和虚拟线程。",
    url: "https://spring.io/blog/2024/12/04/spring-boot-3-2",
    source: "Spring.io",
    author: "Spring Team",
    publishTime: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
    createdAt: new Date().toISOString(),
    likes: 15,
    views: 89,
    tags: "Spring Boot,Java,Backend",
    status: "PUBLISHED"
  },
  {
    id: 3,
    title: "AI-Powered Code Generation: The Future of Development",
    titleZh: "AI 驱动的代码生成：开发的未来",
    summary: "How AI tools like GitHub Copilot are transforming software development.",
    summaryZh: "GitHub Copilot 等 AI 工具如何改变软件开发。",
    url: "https://github.blog/2024/12/03/ai-powered-development",
    source: "GitHub Blog",
    author: "GitHub Team",
    publishTime: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(),
    createdAt: new Date().toISOString(),
    likes: 45,
    views: 234,
    tags: "AI,Machine Learning,Development Tools",
    status: "PUBLISHED"
  }
];

// API路由
app.get('/api/articles', (req, res) => {
  const page = parseInt(req.query.page) || 0;
  const size = parseInt(req.query.size) || 10;
  const status = req.query.status;

  let filteredArticles = mockArticles;
  if (status) {
    filteredArticles = mockArticles.filter(article => article.status === status);
  }

  const startIndex = page * size;
  const endIndex = startIndex + size;
  const paginatedArticles = filteredArticles.slice(startIndex, endIndex);

  res.json({
    content: paginatedArticles,
    totalElements: filteredArticles.length,
    totalPages: Math.ceil(filteredArticles.length / size),
    size: size,
    number: page,
    first: page === 0,
    last: endIndex >= filteredArticles.length
  });
});

app.get('/api/articles/:id', (req, res) => {
  const id = parseInt(req.params.id);
  const article = mockArticles.find(a => a.id === id);
  
  if (article) {
    // 增加浏览量
    article.views += 1;
    res.json(article);
  } else {
    res.status(404).json({ error: 'Article not found' });
  }
});

app.get('/api/articles/search', (req, res) => {
  const keyword = req.query.keyword;
  const page = parseInt(req.query.page) || 0;
  const size = parseInt(req.query.size) || 10;

  const filteredArticles = mockArticles.filter(article => 
    article.title.toLowerCase().includes(keyword.toLowerCase()) ||
    article.titleZh.toLowerCase().includes(keyword.toLowerCase()) ||
    article.summary.toLowerCase().includes(keyword.toLowerCase()) ||
    article.summaryZh.toLowerCase().includes(keyword.toLowerCase())
  );

  const startIndex = page * size;
  const endIndex = startIndex + size;
  const paginatedArticles = filteredArticles.slice(startIndex, endIndex);

  res.json({
    content: paginatedArticles,
    totalElements: filteredArticles.length,
    totalPages: Math.ceil(filteredArticles.length / size),
    size: size,
    number: page,
    first: page === 0,
    last: endIndex >= filteredArticles.length
  });
});

app.get('/api/articles/today', (req, res) => {
  const today = new Date().toDateString();
  const todayArticles = mockArticles.filter(article => 
    new Date(article.publishTime).toDateString() === today
  );
  res.json(todayArticles);
});

app.get('/api/articles/popular', (req, res) => {
  const popularArticles = [...mockArticles]
    .sort((a, b) => b.views - a.views)
    .slice(0, 10);
  res.json(popularArticles);
});

app.post('/api/articles/init-mock-data', (req, res) => {
  res.json('模拟数据初始化成功');
});

app.listen(PORT, () => {
  console.log(`🕷️ IT技术日报系统模拟后端启动成功！`);
  console.log(`访问地址: http://localhost:${PORT}`);
  console.log(`API文档: http://localhost:${PORT}/api/articles`);
});

module.exports = app; 