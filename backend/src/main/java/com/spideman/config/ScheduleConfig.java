package com.spideman.config;

import com.spideman.service.CrawlerService;
import com.spideman.service.DailyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "crawler.schedule.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduleConfig {
    
    private final CrawlerService crawlerService;
    private final DailyReportService dailyReportService;
    
    /**
     * 每天凌晨6点执行：先爬取文章，然后生成当天日报
     */
    @Scheduled(cron = "0 0 6 * * ?") // 每天凌晨6点执行
    public void scheduledCrawlAndGenerateReport() {
        LocalDate today = LocalDate.now();
        log.info("⏰ 定时任务开始：{} 凌晨6点自动爬取文章并生成日报", today);
        
        try {
            // 第一步：爬取文章
            log.info("📡 第一步：开始爬取最新文章...");
            crawlerService.crawlAllSources(5) // 每个源爬取5篇文章
                .thenAccept(result -> {
                    log.info("✅ 文章爬取完成：成功 {} 篇", result.get("totalSuccess"));
                    
                    // 第二步：生成当天日报
                    generateTodayReportAfterCrawl(today);
                })
                .exceptionally(ex -> {
                    log.error("❌ 爬取文章失败", ex);
                    // 即使爬取失败，也尝试生成日报（可能有昨天的文章）
                    generateTodayReportAfterCrawl(today);
                    return null;
                });
                
        } catch (Exception e) {
            log.error("❌ 定时任务执行失败", e);
            // 出现异常时，也尝试生成日报
            generateTodayReportAfterCrawl(today);
        }
    }
    
    /**
     * 爬取完成后生成今日日报
     */
    private void generateTodayReportAfterCrawl(LocalDate targetDate) {
        try {
            log.info("📰 第二步：开始生成 {} 的技术日报...", targetDate);
            
            dailyReportService.generateDailyReport(targetDate);
            
            log.info("✅ 定时任务完成：{} 的文章爬取和日报生成成功", targetDate);
        } catch (Exception e) {
            log.error("❌ 生成 {} 的技术日报失败", targetDate, e);
        }
    }
} 