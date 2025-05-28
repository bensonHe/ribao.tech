package com.spideman.service;

import com.spideman.entity.Article;
import com.spideman.entity.CrawlRecord;
import com.spideman.repository.CrawlRecordRepository;
import com.spideman.service.crawler.WebCrawler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerService {
    
    private final List<WebCrawler> crawlers;
    private final ArticleService articleService;
    private final CrawlRecordRepository crawlRecordRepository;
    
    /**
     * 从所有源爬取文章
     */
    @Async
    public CompletableFuture<Map<String, Object>> crawlAllSources(int articlesPerSource) {
        Map<String, Object> result = new HashMap<>();
        int totalArticles = 0;
        int totalSuccess = 0;
        
        log.info("🚀 开始从所有源爬取文章，每个源 {} 篇", articlesPerSource);
        
        for (WebCrawler crawler : crawlers) {
            if (!crawler.isAvailable()) {
                log.warn("爬虫 {} 不可用，跳过", crawler.getName());
                continue;
            }
            
            CrawlRecord record = new CrawlRecord();
            record.setSource(crawler.getSource());
            record.setCrawlTime(LocalDateTime.now());
            record.setStatus(CrawlRecord.CrawlStatus.RUNNING);
            
            try {
                log.info("📡 正在爬取 {}...", crawler.getSource());
                
                List<Article> articles = crawler.crawlArticles(articlesPerSource);
                int successCount = 0;
                int errorCount = 0;
                
                for (Article article : articles) {
                    try {
                        articleService.saveArticle(article);
                        successCount++;
                        totalSuccess++;
                    } catch (Exception e) {
                        errorCount++;
                        log.warn("保存文章失败: {}", e.getMessage());
                    }
                    totalArticles++;
                }
                
                // 更新爬取记录
                record.setTotalCrawled(articles.size());
                record.setSuccessCount(successCount);
                record.setErrorCount(errorCount);
                record.setStatus(CrawlRecord.CrawlStatus.COMPLETED);
                
                log.info("✅ {}: 爬取 {} 篇，成功 {} 篇，失败 {} 篇", 
                    crawler.getSource(), articles.size(), successCount, errorCount);
                
            } catch (Exception e) {
                record.setStatus(CrawlRecord.CrawlStatus.FAILED);
                record.setErrorMessage(e.getMessage());
                log.error("❌ {} 爬取失败: {}", crawler.getSource(), e.getMessage());
            } finally {
                crawlRecordRepository.save(record);
            }
        }
        
        result.put("totalAttempted", totalArticles);
        result.put("totalSuccess", totalSuccess);
        result.put("totalFailed", totalArticles - totalSuccess);
        result.put("crawlTime", LocalDateTime.now());
        
        log.info("🎉 爬取完成！总计: {} 篇，成功: {} 篇", totalArticles, totalSuccess);
        return CompletableFuture.completedFuture(result);
    }
    
    /**
     * 从指定源爬取文章
     */
    public Map<String, Object> crawlFromSource(String sourceName, int limit) {
        Map<String, Object> result = new HashMap<>();
        
        WebCrawler targetCrawler = crawlers.stream()
            .filter(crawler -> crawler.getSource().equalsIgnoreCase(sourceName))
            .findFirst()
            .orElse(null);
        
        if (targetCrawler == null) {
            result.put("success", false);
            result.put("message", "未找到指定的爬虫源: " + sourceName);
            return result;
        }
        
        CrawlRecord record = new CrawlRecord();
        record.setSource(targetCrawler.getSource());
        record.setCrawlTime(LocalDateTime.now());
        record.setStatus(CrawlRecord.CrawlStatus.RUNNING);
        
        try {
            log.info("📡 正在从 {} 爬取 {} 篇文章...", sourceName, limit);
            
            List<Article> articles = targetCrawler.crawlArticles(limit);
            int successCount = 0;
            
            for (Article article : articles) {
                try {
                    articleService.saveArticle(article);
                    successCount++;
                } catch (Exception e) {
                    log.warn("保存文章失败: {}", e.getMessage());
                }
            }
            
            record.setTotalCrawled(articles.size());
            record.setSuccessCount(successCount);
            record.setErrorCount(articles.size() - successCount);
            record.setStatus(CrawlRecord.CrawlStatus.COMPLETED);
            
            result.put("success", true);
            result.put("source", sourceName);
            result.put("totalCrawled", articles.size());
            result.put("successCount", successCount);
            result.put("errorCount", articles.size() - successCount);
            
            log.info("✅ {} 爬取完成: {} 篇，成功 {} 篇", sourceName, articles.size(), successCount);
            
        } catch (Exception e) {
            record.setStatus(CrawlRecord.CrawlStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            
            result.put("success", false);
            result.put("message", "爬取失败: " + e.getMessage());
            
            log.error("❌ {} 爬取失败: {}", sourceName, e.getMessage());
        } finally {
            crawlRecordRepository.save(record);
        }
        
        return result;
    }
    
    /**
     * 获取可用的爬虫列表
     */
    public List<String> getAvailableCrawlers() {
        return crawlers.stream()
            .filter(WebCrawler::isAvailable)
            .map(WebCrawler::getSource)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 获取爬取统计信息
     */
    public Map<String, Object> getCrawlStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<CrawlRecord> todayRecords = crawlRecordRepository.findTodayRecords();
        List<Object[]> sourceStats = crawlRecordRepository.getSourceStatistics();
        
        int todayCrawls = todayRecords.size();
        int todaySuccess = todayRecords.stream()
            .mapToInt(CrawlRecord::getSuccessCount)
            .sum();
        
        stats.put("todayCrawls", todayCrawls);
        stats.put("todaySuccess", todaySuccess);
        stats.put("availableCrawlers", getAvailableCrawlers());
        stats.put("sourceStatistics", sourceStats);
        
        return stats;
    }
} 