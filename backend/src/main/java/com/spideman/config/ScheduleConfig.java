package com.spideman.config;

import com.spideman.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "crawler.schedule.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduleConfig {
    
    private final CrawlerService crawlerService;
    
    /**
     * 每天早上8点自动爬取技术文章
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduledCrawl() {
        log.info("⏰ 定时任务开始：每日技术文章爬取");
        try {
            crawlerService.crawlAllSources(3)
                .thenAccept(result -> {
                    log.info("✅ 定时爬取完成：成功 {} 篇", result.get("totalSuccess"));
                });
        } catch (Exception e) {
            log.error("❌ 定时爬取失败", e);
        }
    }
    
} 