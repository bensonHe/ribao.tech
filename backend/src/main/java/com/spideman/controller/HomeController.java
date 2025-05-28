package com.spideman.controller;

import com.spideman.dto.ArticleDTO;
import com.spideman.entity.Article;
import com.spideman.service.AlibabaAIService;
import com.spideman.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final ArticleService articleService;
    private final AlibabaAIService aiService;
    private static final Logger log = LoggerFactory.getLogger(HomeController.class);
    
    /**
     * 首页 - 显示最新技术文章
     */
    @GetMapping("/")
    public String home(Model model, 
                      @RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "10") int size) {
        
        // 获取已发布的文章列表
        Pageable pageable = PageRequest.of(page, size);
        Page<ArticleDTO> articles = articleService.getArticles(page, size, Article.ArticleStatus.PUBLISHED);
        
        // 获取统计信息
        long totalArticles = articleService.countAll();
        
        model.addAttribute("articles", articles);
        model.addAttribute("totalArticles", totalArticles);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("hasNext", articles.hasNext());
        model.addAttribute("hasPrevious", articles.hasPrevious());
        
        return "index";
    }
    
    /**
     * 重定向到管理界面（兼容旧链接）
     */
    @GetMapping("/admin")
    public String redirectToAdmin() {
        return "redirect:/spideAdmin/login";
    }
    
    /**
     * 生成每日AI技术日报 - 增强版本
     */
    @PostMapping("/api/daily-report")
    @ResponseBody
    public Map<String, Object> generateDailyReport() {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        
        log.info("🚀 开始生成每日AI技术日报...");
        log.info("📅 生成时间: {}", java.time.LocalDateTime.now());
        
        try {
            // 获取今日文章
            log.info("🔍 查询今日文章...");
            List<Article> todayArticles = articleService.getTodayArticles();
            log.info("📊 查询到今日文章数量: {}", todayArticles.size());
            
            // 记录文章详细信息
            if (!todayArticles.isEmpty()) {
                log.info("📋 今日文章列表:");
                for (int i = 0; i < Math.min(todayArticles.size(), 10); i++) {
                    Article article = todayArticles.get(i);
                    log.info("   {}. [{}] {} (发布时间: {})", 
                        i + 1, 
                        article.getSource(),
                        article.getTitle().length() > 80 ? 
                            article.getTitle().substring(0, 80) + "..." : article.getTitle(),
                        article.getPublishTime());
                }
                if (todayArticles.size() > 10) {
                    log.info("   ... 还有 {} 篇文章", todayArticles.size() - 10);
                }
            } else {
                log.warn("⚠️ 今日无文章，将生成空日报");
            }
            
            // 转换为Object列表以适配AI服务方法
            List<Object> articles = new java.util.ArrayList<>();
            for (Article article : todayArticles) {
                articles.add(article);
            }
            
            // 记录AI调用参数
            log.info("🤖 调用AI服务生成日报...");
            log.info("📋 AI调用参数:");
            log.info("   - 输入文章数: {}", articles.size());
            log.info("   - AI服务: AlibabaAIService.generateDailyReport()");
            log.info("   - 预期模型: qwen-plus");
            
            long aiStartTime = System.currentTimeMillis();
            
            // 生成AI日报
            String dailyReport = aiService.generateDailyReport(articles);
            
            long aiEndTime = System.currentTimeMillis();
            long aiDuration = aiEndTime - aiStartTime;
            
            // 记录AI调用结果
            log.info("🎯 AI日报生成完成:");
            log.info("   - AI调用耗时: {} ms", aiDuration);
            log.info("   - 生成内容长度: {} 字符", dailyReport != null ? dailyReport.length() : 0);
            log.info("   - 内容预览: {}", 
                dailyReport != null && dailyReport.length() > 100 ? 
                    dailyReport.substring(0, 100) + "..." : dailyReport);
            
            // 构建返回结果
            result.put("success", true);
            result.put("report", dailyReport);
            result.put("articleCount", todayArticles.size());
            result.put("generatedAt", java.time.LocalDateTime.now().toString());
            result.put("aiDuration", aiDuration);
            
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("✅ 日报生成成功！总耗时: {} ms (查询: {} ms, AI: {} ms)", 
                totalTime, (aiStartTime - startTime), aiDuration);
            
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("❌ 日报生成失败，总耗时: {} ms", totalTime, e);
            log.error("💥 错误详情: {}", e.getMessage());
            
            result.put("success", false);
            result.put("message", "日报生成失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            result.put("duration", totalTime);
            
            // 记录更详细的错误信息用于调试
            if (e.getCause() != null) {
                log.error("🔍 根因: {}", e.getCause().getMessage());
            }
            
            // 尝试提供降级方案
            result.put("fallbackSuggestion", "请检查网络连接和AI服务配置，或稍后重试");
        }
        
        return result;
    }
} 