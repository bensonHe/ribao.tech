package com.spideman.controller;

import com.spideman.dto.ArticleDTO;
import com.spideman.entity.Article;
import com.spideman.entity.DailyReport;
import com.spideman.entity.VisitRecord;
import com.spideman.service.AlibabaAIService;
import com.spideman.service.ArticleService;
import com.spideman.service.DailyReportService;
import com.spideman.service.VisitStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {
    
    private final ArticleService articleService;
    private final DailyReportService dailyReportService;
    private final AlibabaAIService aiService;
    private final VisitStatisticsService visitStatisticsService;
    
    /**
     * 首页 - 显示今日技术日报
     */
    @GetMapping("/")
    public String home(Model model, HttpServletRequest request) {
        log.info("🏠 访问首页");
        long startTime = System.currentTimeMillis();
        
        // 暂时注释掉访问统计，避免数据库表不存在的问题
        // visitStatisticsService.recordVisit(request, VisitRecord.PageType.HOME);
        
        try {
            // 获取最近10天的日报数据，按日期倒序排列（只包含今天及之前的日期）
            List<DailyReport> recentReports = dailyReportService.getRecentReports(10);
            
            log.info("📰 找到 {} 个最近日报", recentReports.size());
            
            // 获取统计信息
            long totalArticles = articleService.countAll();
            
            model.addAttribute("recentReports", recentReports);
            model.addAttribute("totalArticles", totalArticles);
            model.addAttribute("currentDate", LocalDate.now().toString());
            log.info("🏠 首页加载完成，总耗时: {} ms", System.currentTimeMillis() - startTime);
            return "index";
            
        } catch (Exception e) {
            log.error("❌ 首页加载失败", e);
            model.addAttribute("error", "页面加载失败: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * 日报详情页
     */
    @GetMapping("/report/{reportId}")
    public String reportDetail(@PathVariable Long reportId, Model model, HttpServletRequest request) {
        log.info("📖 访问日报详情: {}", reportId);
        
        // 暂时注释掉访问统计，避免数据库表不存在的问题
        // visitStatisticsService.recordVisit(request, VisitRecord.PageType.REPORT_DETAIL);
        
        try {
            // 直接根据ID查找日报
            log.info("🔍 开始查找日报ID: {}", reportId);
            Optional<DailyReport> reportOpt = dailyReportService.getReportById(reportId);
            log.info("🔍 查找结果: {}", reportOpt.isPresent() ? "找到" : "未找到");
            
            if (!reportOpt.isPresent()) {
                log.warn("⚠️ 日报不存在: {}", reportId);
                model.addAttribute("error", "日报不存在");
                return "error";
            }
            
            DailyReport report = reportOpt.get();
            log.info("📰 找到日报: {} - {}", report.getId(), report.getTitle());
            
            // 增加阅读次数
            dailyReportService.incrementReadCount(reportId);
            
            // 获取相关文章
            List<ArticleDTO> relatedArticles = getRelatedArticles(report);
            
            model.addAttribute("report", report);
            model.addAttribute("relatedArticles", relatedArticles);
            
            log.info("✅ 日报详情页面准备完成");
            return "report-detail";
            
        } catch (Exception e) {
            log.error("❌ 日报详情加载失败: {}", reportId, e);
            model.addAttribute("error", "日报加载失败: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * 文章列表页面
     */
    @GetMapping("/articles")
    public String articles(Model model, 
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String keyword,
                          HttpServletRequest request) {
        
        log.info("📄 访问文章列表页面，页码: {}, 关键词: {}", page, keyword);
        
        // 暂时注释掉访问统计，避免数据库表不存在的问题
        // visitStatisticsService.recordVisit(request, VisitRecord.PageType.ARTICLE_LIST);
        
        try {
            Page<ArticleDTO> articles;
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                // 搜索文章
                articles = articleService.searchArticles(keyword.trim(), page, size);
                model.addAttribute("keyword", keyword);
            } else {
                // 获取所有已发布文章
                articles = articleService.getArticles(page, size, Article.ArticleStatus.PUBLISHED);
            }
            
            model.addAttribute("articles", articles);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", articles.getTotalPages());
            model.addAttribute("hasNext", articles.hasNext());
            model.addAttribute("hasPrevious", articles.hasPrevious());
            
            return "articles";
            
        } catch (Exception e) {
            log.error("❌ 文章列表加载失败", e);
            model.addAttribute("error", "文章列表加载失败: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * 文章详情页
     */
    @GetMapping("/article/{articleId}")
    public String articleDetail(@PathVariable Long articleId, Model model, HttpServletRequest request) {
        log.info("📖 访问文章详情: {}", articleId);
        
        // 暂时注释掉访问统计，避免数据库表不存在的问题
        // visitStatisticsService.recordVisit(request, VisitRecord.PageType.ARTICLE_DETAIL);
        
        try {
            Optional<ArticleDTO> articleOpt = articleService.getArticleById(articleId);
            
            if (!articleOpt.isPresent()) {
                model.addAttribute("error", "文章不存在");
                return "error";
            }
            
            ArticleDTO article = articleOpt.get();
            
            // 增加浏览量
            articleService.incrementViews(articleId);
            
            // 获取热门文章作为推荐
            List<ArticleDTO> recommendedArticles = articleService.getPopularArticles();
            
            model.addAttribute("article", article);
            model.addAttribute("recommendedArticles", recommendedArticles);
            
            return "article-detail";
            
        } catch (Exception e) {
            log.error("❌ 文章详情加载失败: {}", articleId, e);
            model.addAttribute("error", "文章加载失败: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * 重定向到管理界面（兼容旧链接）
     */
    @GetMapping("/admin")
    public String redirectToAdmin() {
        return "redirect:/spideAdmin/login";
    }
    
    /**
     * 生成今日AI技术日报 - API接口
     */
    @PostMapping("/api/daily-report")
    @ResponseBody
    public Map<String, Object> generateTodayReport() {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        
        log.info("🚀 API请求：生成今日AI技术日报");
        
        try {
            // 生成今日日报
            DailyReport report = dailyReportService.generateDailyReport(LocalDate.now());
            
            result.put("success", true);
            result.put("reportId", report.getId());
            result.put("title", report.getTitle());
            result.put("summary", report.getSummary());
            result.put("articleCount", report.getTotalArticles());
            result.put("generatedAt", report.getGeneratedAt().toString());
            
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("✅ 今日日报生成成功！总耗时: {} ms", totalTime);
            
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("❌ 今日日报生成失败，总耗时: {} ms", totalTime, e);
            
            result.put("success", false);
            result.put("message", "日报生成失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            result.put("duration", totalTime);
        }
        
        return result;
    }
    
    /**
     * 生成指定日期的日报 - API接口
     */
    @PostMapping("/api/daily-report/{date}")
    @ResponseBody
    public Map<String, Object> generateReportByDate(@PathVariable String date) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            LocalDate targetDate = LocalDate.parse(date);
            log.info("🚀 API请求：生成 {} 的技术日报", targetDate);
            
            DailyReport report = dailyReportService.generateDailyReport(targetDate);
            
            result.put("success", true);
            result.put("reportId", report.getId());
            result.put("title", report.getTitle());
            result.put("date", report.getReportDate().toString());
            result.put("articleCount", report.getTotalArticles());
            
        } catch (Exception e) {
            log.error("❌ {} 日报生成失败", date, e);
            result.put("success", false);
            result.put("message", "日报生成失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 测试接口：检查日报是否存在
     */
    @GetMapping("/api/test/report/{reportId}")
    @ResponseBody
    public Map<String, Object> testReportExists(@PathVariable Long reportId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<DailyReport> reportOpt = dailyReportService.getReportById(reportId);
            
            result.put("exists", reportOpt.isPresent());
            if (reportOpt.isPresent()) {
                DailyReport report = reportOpt.get();
                result.put("id", report.getId());
                result.put("title", report.getTitle());
                result.put("date", report.getReportDate().toString());
            }
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取相关文章
     */
    private List<ArticleDTO> getRelatedArticles(DailyReport report) {
        try {
            log.info("📰 获取日报相关文章，日期: {}", report.getReportDate());
            
            // 优先获取日报日期当天的文章
            LocalDate reportDate = report.getReportDate();
            LocalDateTime startTime = reportDate.atStartOfDay();
            LocalDateTime endTime = reportDate.plusDays(1).atStartOfDay();
            
            // 如果有关联的文章ID，优先使用
            if (report.getArticleIds() != null && !report.getArticleIds().trim().isEmpty()) {
                try {
                    String[] articleIds = report.getArticleIds().split(",");
                    List<ArticleDTO> specificArticles = new ArrayList<>();
                    
                    for (String idStr : articleIds) {
                        Long articleId = Long.parseLong(idStr.trim());
                        Optional<ArticleDTO> articleOpt = articleService.getArticleById(articleId);
                        if (articleOpt.isPresent()) {
                            specificArticles.add(articleOpt.get());
                        }
                    }
                    
                    if (!specificArticles.isEmpty()) {
                        log.info("✅ 通过关联ID找到 {} 篇文章", specificArticles.size());
                        return specificArticles;
                    }
                } catch (Exception e) {
                    log.warn("解析文章ID失败: {}", report.getArticleIds(), e);
                }
            }
            
            // 获取日报日期前后几天的文章（扩大范围以确保有文章）
            LocalDateTime rangeStart = reportDate.minusDays(2).atStartOfDay();
            LocalDateTime rangeEnd = reportDate.plusDays(2).atStartOfDay();
            
            // 获取时间范围内的文章，转换为DTO
            List<Article> timeRangeArticles = articleService.getArticlesByDateRange(rangeStart, rangeEnd);
            List<ArticleDTO> timeRangeArticleDTOs = timeRangeArticles.stream()
                .map(article -> {
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
                })
                .limit(10) // 限制最多10篇
                .collect(Collectors.toList());
            
            if (!timeRangeArticleDTOs.isEmpty()) {
                log.info("✅ 通过日期范围找到 {} 篇文章", timeRangeArticleDTOs.size());
                return timeRangeArticleDTOs;
            }
            
            // 最后兜底：返回热门文章
            List<ArticleDTO> popularArticles = articleService.getPopularArticles();
            log.info("📄 使用热门文章作为兜底，共 {} 篇", popularArticles.size());
            return popularArticles.size() > 10 ? popularArticles.subList(0, 10) : popularArticles;
            
        } catch (Exception e) {
            log.error("获取相关文章失败", e);
            return Collections.emptyList();
        }
    }
} 