package com.spideman.service;

import com.spideman.entity.Article;
import com.spideman.entity.DailyReport;
import com.spideman.repository.DailyReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyReportService {
    
    private final DailyReportRepository dailyReportRepository;
    private final ArticleService articleService;
    private final AlibabaAIService aiService;
    
    /**
     * 获取今日日报
     */
    public Optional<DailyReport> getTodayReport() {
        LocalDate today = LocalDate.now();
        return dailyReportRepository.findByReportDate(today);
    }
    
    /**
     * 根据日期获取日报
     */
    public Optional<DailyReport> getReportByDate(LocalDate date) {
        return dailyReportRepository.findByReportDate(date);
    }
    
    /**
     * 获取最新发布的日报
     */
    public Optional<DailyReport> getLatestPublishedReport() {
        return dailyReportRepository.findFirstByStatusOrderByReportDateDesc(DailyReport.ReportStatus.PUBLISHED);
    }
    
    /**
     * 分页获取日报列表
     */
    public Page<DailyReport> getReports(Pageable pageable) {
        return dailyReportRepository.findAll(pageable);
    }
    
    /**
     * 根据状态分页获取日报
     */
    public Page<DailyReport> getReportsByStatus(DailyReport.ReportStatus status, Pageable pageable) {
        return dailyReportRepository.findByStatusOrderByReportDateDesc(status, pageable);
    }
    
    /**
     * 获取最近N天的日报（已发布的）
     */
    public List<DailyReport> getRecentReports(int days) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);
        return dailyReportRepository.findByReportDateAfterOrderByReportDateDesc(startDate)
                .stream()
                .filter(report -> report.getStatus() == DailyReport.ReportStatus.PUBLISHED)
                .filter(report -> !report.getReportDate().isAfter(today)) // 不包含未来日期
                .collect(Collectors.toList());
    }
    
    /**
     * 生成指定日期的技术日报
     */
    @Transactional
    public DailyReport generateDailyReport(LocalDate targetDate) {
        long startTime = System.currentTimeMillis();
        log.info("🚀 开始生成 {} 的技术日报", targetDate);
        
        DailyReport report = null;
        try {
            // 检查是否已存在该日期的日报
            Optional<DailyReport> existingReport = dailyReportRepository.findByReportDate(targetDate);
            if (existingReport.isPresent()) {
                report = existingReport.get();
                log.info("📄 发现已存在的日报，将更新内容: {} (ID: {})", targetDate, report.getId());
            } else {
                // 创建新日报
                report = new DailyReport();
                report.setReportDate(targetDate);
                report.setTitle(String.format("%s 技术日报", targetDate.toString()));
                report.setStatus(DailyReport.ReportStatus.DRAFT);
                report.setCreatedAt(LocalDateTime.now());
                report.setReadCount(0);
                
                // 先保存基本信息
                try {
                    report = dailyReportRepository.save(report);
                    log.info("📝 创建新日报记录: {} (ID: {})", targetDate, report.getId());
                } catch (Exception e) {
                    // 如果保存失败，可能是并发创建，重新查询
                    log.warn("创建日报时发生异常，尝试重新查询: {}", e.getMessage());
                    Optional<DailyReport> retryReport = dailyReportRepository.findByReportDate(targetDate);
                    if (retryReport.isPresent()) {
                        report = retryReport.get();
                        log.info("📄 重新查询到已存在的日报: {} (ID: {})", targetDate, report.getId());
                    } else {
                        throw e; // 如果还是没有，抛出原异常
                    }
                }
            }
            
            // 获取指定日期的文章
            List<Article> articles = getArticlesForDate(targetDate);
            log.info("📊 找到 {} 篇文章用于生成日报", articles.size());
            
            if (articles.isEmpty()) {
                // 生成空日报
                generateEmptyReport(report, targetDate);
                log.info("📭 无文章数据，生成空日报");
            } else {
                // 生成AI日报
                generateAIReport(report, articles, targetDate);
            }
            
            // 更新状态为已发布
            report.setStatus(DailyReport.ReportStatus.PUBLISHED);
            report.setUpdatedAt(LocalDateTime.now());
            report = dailyReportRepository.save(report);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ {} 的技术日报生成完成，耗时: {} ms", targetDate, duration);
            
            return report;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ 生成 {} 的技术日报失败，耗时: {} ms", targetDate, duration, e);
            
            // 在新的事务中处理失败状态
            handleReportGenerationFailure(targetDate, e.getMessage());
            
            throw new RuntimeException("日报生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 在新事务中处理日报生成失败
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReportGenerationFailure(LocalDate targetDate, String errorMessage) {
        try {
            Optional<DailyReport> reportOpt = dailyReportRepository.findByReportDate(targetDate);
            if (reportOpt.isPresent()) {
                DailyReport report = reportOpt.get();
                report.setStatus(DailyReport.ReportStatus.DRAFT);
                report.setContent("生成失败：" + errorMessage);
                report.setUpdatedAt(LocalDateTime.now());
                dailyReportRepository.save(report);
                log.info("📝 已更新失败日报状态: {}", targetDate);
            }
        } catch (Exception e) {
            log.warn("更新失败日报状态时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 异步生成今日日报
     */
    @Async
    public void generateTodayReportAsync() {
        try {
            generateDailyReport(LocalDate.now());
        } catch (Exception e) {
            log.error("异步生成今日日报失败", e);
        }
    }
    
    /**
     * 手动更新日报内容
     */
    @Transactional
    public DailyReport updateReport(Long reportId, String title, String summary, String content, 
                                   String highlights, String trends) {
        DailyReport report = dailyReportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("日报不存在"));
        
        report.setTitle(title);
        report.setSummary(summary);
        report.setContent(content);
        report.setHighlights(highlights);
        report.setTrends(trends);
        report.setStatus(DailyReport.ReportStatus.PUBLISHED);
        
        log.info("📝 手动更新日报: {} - {}", report.getReportDate(), title);
        
        return dailyReportRepository.save(report);
    }
    
    /**
     * 增加阅读次数
     */
    @Transactional
    public void incrementReadCount(Long reportId) {
        dailyReportRepository.findById(reportId).ifPresent(report -> {
            report.setReadCount(report.getReadCount() + 1);
            dailyReportRepository.save(report);
        });
    }
    
    /**
     * 删除日报
     */
    @Transactional
    public void deleteReport(Long reportId) {
        dailyReportRepository.deleteById(reportId);
        log.info("🗑️ 删除日报: {}", reportId);
    }
    
    /**
     * 获取指定日期的文章
     */
    private List<Article> getArticlesForDate(LocalDate targetDate) {
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        return articleService.getArticlesByDateRange(startOfDay, endOfDay);
    }
    
    /**
     * 生成空日报（无文章时）
     */
    private void generateEmptyReport(DailyReport report, LocalDate targetDate) {
        String dateStr = targetDate.toString();
        
        report.setTitle(String.format("%s 技术日报", dateStr));
        report.setSummary("今日暂无新文章采集");
        report.setContent(String.format(
            "## 📰 %s 技术日报\n\n" +
            "### 📝 概况\n" +
            "今日暂无新文章采集，请稍后查看。\n\n" +
            "### 💡 建议\n" +
            "- 可以访问管理后台手动触发爬虫任务\n" +
            "- 查看往期日报了解技术趋势\n" +
            "- 关注热门技术社区动态\n\n" +
            "### 📊 统计\n" +
            "- 采集文章数：0 篇\n" +
            "- 生成时间：%s",
            dateStr, LocalDateTime.now()
        ));
        report.setHighlights("暂无");
        report.setTrends("暂无");
        report.setTotalArticles(0);
        report.setArticleIds("");
    }
    
    /**
     * 生成AI日报
     */
    private void generateAIReport(DailyReport report, List<Article> articles, LocalDate targetDate) {
        log.info("🤖 开始调用AI生成日报内容...");
        
        // 转换为Object列表以适配AI服务
        List<Object> articleObjects = articles.stream()
            .map(article -> (Object) article)
            .collect(Collectors.toList());
        
        // 调用AI生成日报
        String aiContent = aiService.generateDailyReport(articleObjects);
        
        // 解析AI生成的内容
        parseAIContent(report, aiContent, articles, targetDate);
        
        // 设置统计信息
        report.setTotalArticles(articles.size());
        report.setArticleIds(articles.stream()
            .map(article -> article.getId().toString())
            .collect(Collectors.joining(",")));
        
        log.info("✅ AI日报内容生成完成，文章数: {}", articles.size());
    }
    
    /**
     * 解析AI生成的内容
     */
    private void parseAIContent(DailyReport report, String aiContent, List<Article> articles, LocalDate targetDate) {
        String dateStr = targetDate.toString();
        
        // 设置标题
        report.setTitle(String.format("%s 技术日报", dateStr));
        
        try {
            // 清理AI返回的内容，去除可能的markdown代码块标记
            String cleanedContent = aiContent.trim();
            
            // 去除开头的 ```json 和结尾的 ```
            if (cleanedContent.startsWith("```json")) {
                cleanedContent = cleanedContent.substring(7); // 移除 ```json
            } else if (cleanedContent.startsWith("```")) {
                cleanedContent = cleanedContent.substring(3); // 移除 ```
            }
            
            if (cleanedContent.endsWith("```")) {
                cleanedContent = cleanedContent.substring(0, cleanedContent.length() - 3); // 移除结尾的 ```
            }
            
            cleanedContent = cleanedContent.trim();
            
            log.info("🧹 清理后的AI内容预览: {}", 
                cleanedContent.length() > 200 ? 
                    cleanedContent.substring(0, 200).replaceAll("\n", " ") + "..." : 
                    cleanedContent.replaceAll("\n", " "));
            
            // 尝试解析JSON格式的AI响应
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(cleanedContent);
            
            // 提取新格式字段
            if (jsonNode.has("todayTrends")) {
                report.setTodayTrends(jsonNode.get("todayTrends").asText());
                log.info("✅ 成功解析今日总结: {} 字符", report.getTodayTrends().length());
            }
            
            if (jsonNode.has("recommendedArticles")) {
                report.setRecommendedArticles(jsonNode.get("recommendedArticles").toString());
                log.info("✅ 成功解析推荐文章: {} 篇", jsonNode.get("recommendedArticles").size());
            }
            
            if (jsonNode.has("dailyQuote")) {
                report.setDailyQuote(jsonNode.get("dailyQuote").asText());
                log.info("✅ 成功解析每日一语: {}", report.getDailyQuote());
            }
            
            if (jsonNode.has("solarTerm")) {
                report.setSolarTerm(jsonNode.get("solarTerm").asText());
                log.info("✅ 成功解析节气: {}", report.getSolarTerm());
            }
            
            // 生成完整的Markdown内容（用于兼容性）
            StringBuilder markdownContent = new StringBuilder();
            markdownContent.append("## 📰 ").append(dateStr).append(" 技术日报\n\n");
            
            // 今日总结
            if (report.getTodayTrends() != null) {
                markdownContent.append("### 📈 今日总结\n\n");
                markdownContent.append(report.getTodayTrends()).append("\n\n");
            }
            
            // 推荐文章
            if (report.getRecommendedArticles() != null) {
                markdownContent.append("### 📚 今日优质文章推荐\n\n");
                try {
                    com.fasterxml.jackson.databind.JsonNode articlesNode = objectMapper.readTree(report.getRecommendedArticles());
                    if (articlesNode.isArray()) {
                        int index = 1;
                        for (com.fasterxml.jackson.databind.JsonNode articleNode : articlesNode) {
                            markdownContent.append("#### ").append(index++).append(". ")
                                .append(articleNode.get("title").asText()).append("\n\n");
                            markdownContent.append("**🔗 链接：** ").append(articleNode.get("url").asText()).append("\n\n");
                            markdownContent.append("**📝 简介：** ").append(articleNode.get("summary").asText()).append("\n\n");
                            markdownContent.append("**💡 推荐理由：** ").append(articleNode.get("reason").asText()).append("\n\n");
                            markdownContent.append("---\n\n");
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析推荐文章JSON失败", e);
                }
            }
            
            // 每日一语
            if (report.getDailyQuote() != null) {
                markdownContent.append("### 🌟 每日一语\n\n");
                markdownContent.append("> ").append(report.getDailyQuote()).append("\n\n");
            }
            
            markdownContent.append("---\n");
            markdownContent.append("*📅 生成时间：").append(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("*");
            
            report.setContent(markdownContent.toString());
            
            // 生成摘要（取今日总结的前100字符）
            String summary = report.getTodayTrends() != null ? 
                (report.getTodayTrends().length() > 100 ? 
                    report.getTodayTrends().substring(0, 100) + "..." : 
                    report.getTodayTrends()) : 
                "今日技术日报";
            report.setSummary(summary);
            
            log.info("✅ 成功解析AI生成的JSON格式日报");
            
        } catch (Exception e) {
            log.warn("⚠️ 解析JSON格式失败，使用原始内容: {}", e.getMessage());
            log.warn("🔍 解析失败的内容: {}", aiContent.length() > 500 ? aiContent.substring(0, 500) + "..." : aiContent);
            
            // 如果JSON解析失败，使用原始内容
            report.setContent(aiContent);
            
            // 生成摘要（取AI内容的前200字符）
            String summary = aiContent.length() > 200 ? 
                aiContent.substring(0, 200) + "..." : aiContent;
            report.setSummary(summary);
            
            // 设置默认值
            report.setTodayTrends("今日总结解析失败，请查看完整内容");
            report.setDailyQuote("今天也要加油哦！");
            report.setSolarTerm(getSolarTerm(targetDate));
        }
        
        // 尝试提取亮点和趋势（简单的文本解析）
        extractHighlightsAndTrends(report, report.getContent(), articles);
    }
    
    /**
     * 获取当前节气
     */
    private String getSolarTerm(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        
        // 简化的节气判断（基于大致日期）
        if (month == 1) {
            if (day < 6) return "小寒";
            else if (day < 20) return "大寒";
            else return "立春";
        } else if (month == 2) {
            if (day < 4) return "立春";
            else if (day < 19) return "雨水";
            else return "惊蛰";
        } else if (month == 3) {
            if (day < 6) return "惊蛰";
            else if (day < 21) return "春分";
            else return "清明";
        } else if (month == 4) {
            if (day < 5) return "清明";
            else if (day < 20) return "谷雨";
            else return "立夏";
        } else if (month == 5) {
            if (day < 6) return "立夏";
            else if (day < 21) return "小满";
            else return "芒种";
        } else if (month == 6) {
            if (day < 6) return "芒种";
            else if (day < 22) return "夏至";
            else return "小暑";
        } else if (month == 7) {
            if (day < 7) return "小暑";
            else if (day < 23) return "大暑";
            else return "立秋";
        } else if (month == 8) {
            if (day < 8) return "立秋";
            else if (day < 23) return "处暑";
            else return "白露";
        } else if (month == 9) {
            if (day < 8) return "白露";
            else if (day < 23) return "秋分";
            else return "寒露";
        } else if (month == 10) {
            if (day < 9) return "寒露";
            else if (day < 24) return "霜降";
            else return "立冬";
        } else if (month == 11) {
            if (day < 8) return "立冬";
            else if (day < 22) return "小雪";
            else return "大雪";
        } else { // month == 12
            if (day < 7) return "大雪";
            else if (day < 22) return "冬至";
            else return "小寒";
        }
    }
    
    /**
     * 提取亮点和趋势
     */
    private void extractHighlightsAndTrends(DailyReport report, String content, List<Article> articles) {
        // 简单的关键词提取作为亮点
        StringBuilder highlights = new StringBuilder();
        StringBuilder trends = new StringBuilder();
        
        // 从文章标题中提取热门技术关键词
        List<String> techKeywords = articles.stream()
            .map(Article::getTitle)
            .flatMap(title -> java.util.Arrays.stream(title.split("\\s+")))
            .filter(word -> isTechKeyword(word))
            .distinct()
            .limit(10)
            .collect(Collectors.toList());
        
        if (!techKeywords.isEmpty()) {
            highlights.append("热门技术关键词：").append(String.join(", ", techKeywords));
        }
        
        // 统计文章来源
        java.util.Map<String, Long> sourceCount = articles.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                Article::getSource, 
                java.util.stream.Collectors.counting()
            ));
        
        if (!sourceCount.isEmpty()) {
            trends.append("主要信息源：");
            sourceCount.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> trends.append(entry.getKey()).append("(").append(entry.getValue()).append("篇) "));
        }
        
        report.setHighlights(highlights.toString());
        report.setTrends(trends.toString());
    }
    
    /**
     * 判断是否为技术关键词
     */
    private boolean isTechKeyword(String word) {
        String[] techWords = {
            "React", "Vue", "Angular", "JavaScript", "TypeScript", "Node.js", "Python", "Java", 
            "Spring", "Docker", "Kubernetes", "AWS", "Azure", "AI", "ML", "API", "REST", "GraphQL",
            "MongoDB", "MySQL", "Redis", "Microservices", "DevOps", "CI/CD", "Git", "GitHub"
        };
        
        return java.util.Arrays.stream(techWords)
            .anyMatch(tech -> tech.equalsIgnoreCase(word));
    }
    
    /**
     * 保存日报
     */
    @Transactional
    public DailyReport saveReport(DailyReport report) {
        if (report.getCreatedAt() == null) {
            report.setCreatedAt(LocalDateTime.now());
        }
        report.setUpdatedAt(LocalDateTime.now());
        
        DailyReport savedReport = dailyReportRepository.save(report);
        log.info("📝 保存日报: {} - {}", savedReport.getReportDate(), savedReport.getTitle());
        return savedReport;
    }
    
    /**
     * 根据ID获取日报
     */
    public Optional<DailyReport> getReportById(Long id) {
        return dailyReportRepository.findById(id);
    }
} 