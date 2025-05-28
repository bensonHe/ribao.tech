package com.spideman.controller;

import com.spideman.dto.ArticleDTO;
import com.spideman.entity.Article;
import com.spideman.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:3000") // 允许前端跨域访问
public class ArticleController {
    
    private final ArticleService articleService;
    
    /**
     * 获取文章列表
     */
    @GetMapping
    public ResponseEntity<Page<ArticleDTO>> getArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        
        Article.ArticleStatus articleStatus = null;
        if (status != null) {
            try {
                articleStatus = Article.ArticleStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("无效的文章状态: {}", status);
            }
        }
        
        Page<ArticleDTO> articles = articleService.getArticles(page, size, articleStatus);
        return ResponseEntity.ok(articles);
    }
    
    /**
     * 根据ID获取文章详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArticleDTO> getArticleById(@PathVariable Long id) {
        Optional<ArticleDTO> article = articleService.getArticleById(id);
        if (article.isPresent()) {
            // 增加浏览量
            articleService.incrementViews(id);
            return ResponseEntity.ok(article.get());
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * 搜索文章
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ArticleDTO>> searchArticles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<ArticleDTO> articles = articleService.searchArticles(keyword, page, size);
        return ResponseEntity.ok(articles);
    }
    
    /**
     * 获取今日文章
     */
    @GetMapping("/today")
    public ResponseEntity<List<ArticleDTO>> getTodayArticles() {
        List<ArticleDTO> articles = articleService.getTodayArticlesDTO();
        return ResponseEntity.ok(articles);
    }
    
    /**
     * 获取热门文章
     */
    @GetMapping("/popular")
    public ResponseEntity<List<ArticleDTO>> getPopularArticles() {
        List<ArticleDTO> articles = articleService.getPopularArticles();
        return ResponseEntity.ok(articles);
    }
    
    /**
     * 初始化模拟数据（用于测试）
     */
    @PostMapping("/init-mock-data")
    public ResponseEntity<String> initMockData() {
        try {
            articleService.createMockData();
            return ResponseEntity.ok("模拟数据初始化成功");
        } catch (Exception e) {
            log.error("初始化模拟数据失败", e);
            return ResponseEntity.internalServerError().body("初始化失败: " + e.getMessage());
        }
    }
} 