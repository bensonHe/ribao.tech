package com.spideman;

import com.spideman.service.ArticleService;
import com.spideman.service.DailyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDate;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@RequiredArgsConstructor
@Slf4j
public class SpidemanApplication implements CommandLineRunner {

    private final ArticleService articleService;
    private final DailyReportService dailyReportService;

    public static void main(String[] args) {
        SpringApplication.run(SpidemanApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 TechDaily 应用启动完成！");
        log.info("🌐 访问地址: http://localhost:8080");
        log.info("🔧 管理后台: http://localhost:8080/spideAdmin/login");
        
        // 异步执行数据初始化，不阻塞应用启动
        initializeDataAsync();
    }
    @Async
    public void initializeDataAsync() {
        log.info("🔄 开始异步初始化数据...");
        long startTime = System.currentTimeMillis();
        
        try {
            // 检查数据库中是否已有数据
            long articleCount = articleService.countAll();
            log.info("📊 当前数据库中有 {} 篇文章", articleCount);
            
            if (articleCount == 0) {
                log.info("📝 数据库为空，开始创建示例数据...");
                articleService.createMockData();
                log.info("✅ 示例数据创建完成");
            } else {
                
                log.info("📚 数据库已有数据，跳过示例数据创建");
            }
            
            // 检查是否有今日日报，如果没有则生成
            if (!dailyReportService.getTodayReport().isPresent()) {
                log.info("📰 今日日报不存在，开始生成...");
                try {
                    dailyReportService.generateDailyReport(LocalDate.now());
                    log.info("✅ 今日日报生成成功");
                } catch (Exception e) {
                    log.warn("⚠️ 今日日报生成失败，将在后续自动重试: {}", e.getMessage());
                }
            } else {
                log.info("📰 今日日报已存在");
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("🎉 数据初始化完成，总耗时: {}ms", totalTime);
            
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("❌ 数据初始化失败，耗时: {}ms", totalTime, e);
        }
    }
} 