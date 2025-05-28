package com.spideman.controller;

import com.spideman.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CrawlerController {
    
    private final CrawlerService crawlerService;
    
    /**
     * 快速爬取模式 - 从所有源各爬取2篇文章
     */
    @PostMapping("/quick-crawl")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> quickCrawl() {
        return crawlerService.crawlAllSources(2)
            .thenApply(result -> ResponseEntity.ok(result));
    }
    
    /**
     * 完整爬取模式 - 从所有源爬取指定数量文章
     */
    @PostMapping("/full-crawl")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> fullCrawl(
            @RequestParam(defaultValue = "5") int articlesPerSource) {
        return crawlerService.crawlAllSources(articlesPerSource)
            .thenApply(result -> ResponseEntity.ok(result));
    }
    
    /**
     * 从指定源爬取文章
     */
    @PostMapping("/crawl-source")
    public ResponseEntity<Map<String, Object>> crawlFromSource(
            @RequestParam String source,
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> result = crawlerService.crawlFromSource(source, limit);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取可用的爬虫列表
     */
    @GetMapping("/sources")
    public ResponseEntity<Map<String, Object>> getAvailableSources() {
        Map<String, Object> response = new HashMap<>();
        response.put("sources", crawlerService.getAvailableCrawlers());
        response.put("message", "可用的爬虫源列表");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取爬取统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getCrawlStatistics() {
        Map<String, Object> stats = crawlerService.getCrawlStatistics();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "ok");
        health.put("availableCrawlers", crawlerService.getAvailableCrawlers().size());
        health.put("message", "爬虫服务运行正常");
        return ResponseEntity.ok(health);
    }
} 