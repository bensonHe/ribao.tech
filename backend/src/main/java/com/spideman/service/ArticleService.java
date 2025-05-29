package com.spideman.service;

import com.spideman.dto.ArticleDTO;
import com.spideman.entity.Article;
import com.spideman.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {
    
    private final ArticleRepository articleRepository;
    
    /**
     * 获取文章列表（分页）
     */
    public Page<ArticleDTO> getArticles(int page, int size, Article.ArticleStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Article> articles = status != null ? 
            articleRepository.findByStatusOrderByPublishTimeDesc(status, pageable) :
            articleRepository.findAll(pageable);
        
        return articles.map(this::convertToDTO);
    }
    
    /**
     * 根据ID获取文章详情
     */
    public Optional<ArticleDTO> getArticleById(Long id) {
        return articleRepository.findById(id)
            .map(this::convertToDTO);
    }
    
    /**
     * 保存文章
     */
    @Transactional
    public ArticleDTO saveArticle(Article article) {
        // 检查URL是否已存在
        Optional<Article> existing = articleRepository.findByUrl(article.getUrl());
        if (existing.isPresent()) {
            log.warn("文章已存在: {}", article.getUrl());
            return convertToDTO(existing.get());
        }
        
        Article saved = articleRepository.save(article);
        log.info("保存文章: {} - {}", saved.getId(), saved.getTitle());
        return convertToDTO(saved);
    }
    
    /**
     * 更新文章浏览量
     */
    @Transactional
    public void incrementViews(Long articleId) {
        articleRepository.findById(articleId).ifPresent(article -> {
            article.setViews(article.getViews() + 1);
            articleRepository.save(article);
        });
    }
    
    /**
     * 搜索文章
     */
    public Page<ArticleDTO> searchArticles(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Article> articles = articleRepository.searchArticles(keyword, pageable);
        return articles.map(this::convertToDTO);
    }
    
    /**
     * 获取今日文章（DTO格式）
     */
    public List<ArticleDTO> getTodayArticlesDTO() {
        List<Article> articles = getTodayArticles(); // 调用增强的方法
        return articles.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取热门文章
     */
    public List<ArticleDTO> getPopularArticles() {
        List<Article> articles = articleRepository.findTop10ByStatusOrderByViewsDesc(Article.ArticleStatus.PUBLISHED);
        return articles.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * 创建模拟数据（用于测试）
     */
    @Transactional
    public void createMockData() {
        if (articleRepository.count() == 0) {
            log.info("创建模拟数据...");
            
            Article article1 = new Article();
            article1.setTitle("React 19 Beta: New Features and Improvements");
            article1.setTitleZh("React 19 Beta：新功能和改进");
            article1.setSummary("React 19 introduces new hooks, server components, and performance improvements.");
            article1.setSummaryZh("React 19 引入了新的 hooks、服务器组件和性能改进。");
            article1.setUrl("https://react.dev/blog/2024/12/05/react-19-beta");
            article1.setSource("React.dev");
            article1.setAuthor("React Team");
            article1.setPublishTime(LocalDateTime.now().minusDays(1));
            article1.setTags("React,Frontend,JavaScript");
            article1.setStatus(Article.ArticleStatus.PUBLISHED);
            article1.setViews(156);
            article1.setLikes(23);
            
            Article article2 = new Article();
            article2.setTitle("Spring Boot 3.2: What's New");
            article2.setTitleZh("Spring Boot 3.2：新特性介绍");
            article2.setSummary("Spring Boot 3.2 brings GraalVM native image support and virtual threads.");
            article2.setSummaryZh("Spring Boot 3.2 带来了 GraalVM 本地镜像支持和虚拟线程。");
            article2.setUrl("https://spring.io/blog/2024/12/04/spring-boot-3-2");
            article2.setSource("Spring.io");
            article2.setAuthor("Spring Team");
            article2.setPublishTime(LocalDateTime.now().minusDays(2));
            article2.setTags("Spring Boot,Java,Backend");
            article2.setStatus(Article.ArticleStatus.PUBLISHED);
            article2.setViews(89);
            article2.setLikes(15);
            
            Article article3 = new Article();
            article3.setTitle("AI-Powered Code Generation: The Future of Development");
            article3.setTitleZh("AI 驱动的代码生成：开发的未来");
            article3.setSummary("How AI tools like GitHub Copilot are transforming software development.");
            article3.setSummaryZh("GitHub Copilot 等 AI 工具如何改变软件开发。");
            article3.setUrl("https://github.blog/2024/12/03/ai-powered-development");
            article3.setSource("GitHub Blog");
            article3.setAuthor("GitHub Team");
            article3.setPublishTime(LocalDateTime.now().minusDays(3));
            article3.setTags("AI,Machine Learning,Development Tools");
            article3.setStatus(Article.ArticleStatus.PUBLISHED);
            article3.setViews(234);
            article3.setLikes(45);
            
            articleRepository.saveAll(Arrays.asList(article1, article2, article3));
            log.info("模拟数据创建完成");
        }
    }
    
    /**
     * 获取文章总数
     */
    public long countAll() {
        return articleRepository.count();
    }
    
    /**
     * 获取文章列表（分页，支持Pageable参数）
     */
    public Page<Article> getArticles(Pageable pageable) {
        return articleRepository.findAll(pageable);
    }
    
    /**
     * 根据来源分页查询文章
     */
    public Page<Article> getArticlesBySource(String source, Pageable pageable) {
        return articleRepository.findBySource(source, pageable);
    }
    
    /**
     * 搜索文章（支持Pageable参数）
     */
    public Page<Article> searchArticles(String keyword, Pageable pageable) {
        return articleRepository.searchArticles(keyword, pageable);
    }
    
    /**
     * 删除文章
     */
    @Transactional
    public void deleteArticle(Long id) {
        articleRepository.deleteById(id);
        log.info("删除文章: {}", id);
    }
    
    /**
     * 获取今日文章（原始Entity格式）- 增强版本
     */
    public List<Article> getTodayArticles() {
        long startTime = System.currentTimeMillis();
        log.info("🔍 开始查询今日文章...");
        
        // 计算今日开始和结束时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        log.info("📅 查询时间范围: {} 到 {}", startOfDay, endOfDay);
        log.info("🕐 当前时间: {}", now);
        
        try {
            // 使用改进的参数化查询
            List<Article> todayArticles = articleRepository.findTodayArticles(startOfDay, endOfDay);
            
            // 统计今日文章数量
            long todayCount = articleRepository.countTodayArticles(startOfDay, endOfDay);
            
            // 查询最近3天的文章作为对比
            LocalDateTime threeDaysAgo = now.minusDays(3);
            List<Article> recentArticles = articleRepository.findRecentArticles(threeDaysAgo);
            
            // 查询所有已发布文章数量
            long totalPublished = articleRepository.findByStatus(Article.ArticleStatus.PUBLISHED).size();
            
            long queryTime = System.currentTimeMillis() - startTime;
            
            log.info("📊 查询结果统计:");
            log.info("   - 今日文章数量: {} 篇", todayCount);
            log.info("   - 实际返回文章: {} 篇", todayArticles.size());
            log.info("   - 最近3天文章: {} 篇", recentArticles.size());
            log.info("   - 总发布文章: {} 篇", totalPublished);
            log.info("   - 查询耗时: {} ms", queryTime);
            
            // 如果今日文章为空，输出详细调试信息
            if (todayArticles.isEmpty()) {
                log.warn("⚠️ 今日文章查询结果为空，开始详细分析...");
                
                // 尝试使用原始查询方法
                List<Article> oldResult = articleRepository.findTodayArticlesOld();
                log.info("🔄 使用原始查询方法结果: {} 篇", oldResult.size());
                
                // 输出最近几篇文章的时间信息
                if (!recentArticles.isEmpty()) {
                    log.info("📋 最近文章时间分布:");
                    recentArticles.stream()
                        .limit(10)
                        .forEach(article -> {
                            log.info("   - ID: {}, 标题: {}, 发布时间: {}", 
                                article.getId(), 
                                article.getTitle().length() > 50 ? 
                                    article.getTitle().substring(0, 50) + "..." : article.getTitle(),
                                article.getPublishTime());
                        });
                }
                
                // 如果没有找到今日文章，但有最近文章，返回最近3天的
                if (!recentArticles.isEmpty()) {
                    log.info("🔄 今日无文章，返回最近3天文章 {} 篇", recentArticles.size());
                    return recentArticles.stream()
                        .limit(20)  // 限制返回数量
                        .collect(Collectors.toList());
                }
            } else {
                log.info("✅ 成功查询到今日文章，返回 {} 篇", todayArticles.size());
                
                // 输出前几篇文章的基本信息
                todayArticles.stream()
                    .limit(5)
                    .forEach(article -> {
                        log.info("   📄 ID: {}, 标题: {}, 发布时间: {}", 
                            article.getId(), 
                            article.getTitle().length() > 50 ? 
                                article.getTitle().substring(0, 50) + "..." : article.getTitle(),
                            article.getPublishTime());
                    });
            }
            
            return todayArticles;
            
        } catch (Exception e) {
            long queryTime = System.currentTimeMillis() - startTime;
            log.error("❌ 查询今日文章失败，耗时: {} ms", queryTime, e);
            
            // 发生异常时，尝试返回最近文章
            try {
                log.info("🔄 尝试查询最近文章作为备选...");
                LocalDateTime twoDaysAgo = now.minusDays(2);
                List<Article> fallbackArticles = articleRepository.findRecentArticles(twoDaysAgo);
                log.info("🔄 备选查询返回 {} 篇文章", fallbackArticles.size());
                return fallbackArticles.stream().limit(10).collect(Collectors.toList());
            } catch (Exception fallbackException) {
                log.error("❌ 备选查询也失败", fallbackException);
                return Arrays.asList(); // 返回空列表
            }
        }
    }
    
    /**
     * 根据日期范围获取文章
     */
    public List<Article> getArticlesByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        log.info("🔍 查询日期范围内的文章: {} 到 {}", startTime, endTime);
        
        try {
            List<Article> articles = articleRepository.findByPublishTimeBetween(startTime, endTime);
            log.info("📊 找到 {} 篇文章", articles.size());
            return articles;
        } catch (Exception e) {
            log.error("❌ 查询日期范围内的文章失败", e);
            return Arrays.asList();
        }
    }
    
    /**
     * 转换为DTO
     */
    private ArticleDTO convertToDTO(Article article) {
        ArticleDTO dto = new ArticleDTO();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setTitleZh(article.getTitleZh());
        dto.setSummary(article.getSummary());
        dto.setSummaryZh(article.getSummaryZh());
        dto.setUrl(article.getUrl());
        dto.setSource(article.getSource());
        dto.setAuthor(article.getAuthor());
        dto.setPublishTime(article.getPublishTime());
        dto.setCreatedAt(article.getCreatedAt());
        dto.setLikes(article.getLikes());
        dto.setViews(article.getViews());
        dto.setTags(article.getTags());
        dto.setStatus(article.getStatus().name());
        return dto;
    }
} 