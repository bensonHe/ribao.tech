package com.spideman.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AlibabaAIService {

    @Value("${alibaba.ai.api-key}")
    private String apiKey;

    @Value("${alibaba.ai.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 翻译文章标题
     */
    public String translateTitle(String title) {
        try {
            String prompt = String.format(
                "请将以下英文标题翻译成中文，保持专业性和准确性：\n\n%s\n\n" +
                "要求：\n" +
                "1. 翻译要准确、自然\n" +
                "2. 保持技术术语的专业性\n" +
                "3. 只返回翻译结果，不要其他内容", 
                title
            );

            return callAlibabaAI("qwen-turbo", prompt);
        } catch (Exception e) {
            log.error("翻译标题失败: title={}", title, e);
            return "翻译失败：" + e.getMessage();
        }
    }

    /**
     * 生成每日技术日报 - 增强版本
     */
    public String generateDailyReport(java.util.List<Object> todayArticles) {
        long startTime = System.currentTimeMillis();
        log.info("🤖 AI服务开始生成日报...");
        
        try {
            if (todayArticles == null || todayArticles.isEmpty()) {
                log.warn("⚠️ 输入文章列表为空，生成默认日报");
                return "## 📰 今日技术日报\n\n### 📝 概况\n今日暂无新文章采集，请稍后查看。\n\n### 💡 建议\n可以访问管理后台手动触发爬虫任务。";
            }

            log.info("📊 输入数据统计:");
            log.info("   - 文章总数: {}", todayArticles.size());
            log.info("   - 预处理文章数量: {}", Math.min(todayArticles.size(), 20));

            StringBuilder articlesText = new StringBuilder();
            int processedCount = 0;
            
            for (int i = 0; i < Math.min(todayArticles.size(), 20); i++) {
                Object articleObj = todayArticles.get(i);
                if (articleObj instanceof com.spideman.entity.Article) {
                    com.spideman.entity.Article article = (com.spideman.entity.Article) articleObj;
                    
                    String articleEntry = String.format(
                        "%d. 【%s】%s\n   来源：%s | 作者：%s\n   链接：%s\n   摘要：%s\n\n",
                        i + 1, article.getSource(), article.getTitle(),
                        article.getSource(), article.getAuthor() != null ? article.getAuthor() : "未知",
                        article.getUrl(), article.getSummary() != null ? article.getSummary() : "暂无摘要"
                    );
                    
                    articlesText.append(articleEntry);
                    processedCount++;
                    
                    // 记录处理的文章信息
                    if (i < 5) { // 只记录前5篇的详细信息
                        log.info("   📄 处理文章 {}: [{}] {}", 
                            i + 1, 
                            article.getSource(),
                            article.getTitle().length() > 60 ? 
                                article.getTitle().substring(0, 60) + "..." : article.getTitle());
                    }
                }
            }
            
            log.info("✅ 文章预处理完成，实际处理: {} 篇", processedCount);

            String currentDate = java.time.LocalDate.now().toString();
            String prompt = String.format(
                "请基于以下今日采集的技术文章，生成一份专业的技术日报总结。\n\n" +
                "文章列表：\n%s\n\n" +
                "请按照以下格式生成日报：\n\n" +
                "## 📰 今日技术日报 (%s)\n\n" +
                "### 🔥 热门话题\n" +
                "[分析今日文章中出现频率较高的技术话题、趋势]\n\n" +
                "### 💡 技术趋势\n" +
                "[分析当前技术发展趋势、新兴技术、行业动态]\n\n" +
                "### 🎯 关键洞察\n" +
                "[提炼出的关键技术洞察和行业观点]\n\n" +
                "### 💡 值得关注的文章推荐\n" +
                "[列出今日值得关注的文章，并说明推荐理由]\n\n",
                articlesText.toString(),
                currentDate
            );

            log.info("📝 AI提示词统计:");
            log.info("   - 提示词长度: {} 字符", prompt.length());
            log.info("   - 目标模型: qwen-plus");
            log.info("   - 预期输出: Markdown格式日报");

            long aiCallStart = System.currentTimeMillis();
            log.info("🚀 开始调用AI模型...");

            String result = callAlibabaAI("qwen-plus", prompt);
            
            long aiCallEnd = System.currentTimeMillis();
            long aiCallDuration = aiCallEnd - aiCallStart;
            long totalDuration = aiCallEnd - startTime;

            log.info("🎯 AI调用完成:");
            log.info("   - AI调用耗时: {} ms", aiCallDuration);
            log.info("   - 总处理耗时: {} ms", totalDuration);
            log.info("   - 返回内容长度: {} 字符", result != null ? result.length() : 0);
            log.info("   - 返回内容预览: {}", 
                result != null && result.length() > 150 ? 
                    result.substring(0, 150).replaceAll("\n", " ") + "..." : 
                    (result != null ? result.replaceAll("\n", " ") : "null"));

            return result;

        } catch (Exception e) {
            long failedDuration = System.currentTimeMillis() - startTime;
            log.error("❌ AI日报生成失败，耗时: {} ms", failedDuration, e);
            log.error("💥 错误类型: {}", e.getClass().getSimpleName());
            log.error("💥 错误信息: {}", e.getMessage());
            
            if (e.getCause() != null) {
                log.error("🔍 根本原因: {}", e.getCause().getMessage());
            }
            
            // 返回包含错误信息的日报
            return String.format(
                "## 📰 今日技术日报\n\n" +
                "### ❌ 生成失败\n" +
                "错误信息: %s\n\n" +
                "### 📊 统计信息\n" +
                "- 输入文章数: %d\n" +
                "- 处理耗时: %d ms\n" +
                "- 错误类型: %s\n\n" +
                "### 📝 说明\n" +
                "请检查AI服务配置或稍后重试。如果问题持续，请联系管理员。",
                e.getMessage(), 
                todayArticles != null ? todayArticles.size() : 0,
                failedDuration,
                e.getClass().getSimpleName()
            );
        }
    }

    /**
     * 总结文章内容
     */
    public String summarizeArticle(String articleUrl, String title) {
        try {
            // 1. 抓取文章内容
            String articleContent = fetchArticleContent(articleUrl);
            if (articleContent == null || articleContent.trim().isEmpty()) {
                return "无法获取文章内容，请检查链接是否有效";
            }

            // 2. 调用AI进行总结
            String prompt = String.format(
                "请对以下技术文章进行详细的阅读和分析，并提供一个结构化的总结：\n\n" +
                "文章标题：%s\n" +
                "文章链接：%s\n\n" +
                "文章内容：\n%s\n\n" +
                "请按照以下格式提供总结：\n\n" +
                "## 📋 内容概述\n" +
                "[3-4句话概述文章主要内容]\n\n" +
                "## 🔍 核心要点\n" +
                "• [要点1]\n" +
                "• [要点2]\n" +
                "• [要点3]\n" +
                "• [要点4]\n\n" +
                "## 💡 技术亮点\n" +
                "[介绍文章中的技术创新、解决方案或最佳实践]\n\n" +
                "## 🎯 适用场景\n" +
                "[说明这些技术或方法适用于什么场景]\n\n" +
                "## 📚 价值评估\n" +
                "[评估这篇文章对读者的价值和重要性]",
                title, articleUrl, truncateContent(articleContent, 8000)
            );

            return callAlibabaAI("qwen-plus", prompt);
        } catch (Exception e) {
            log.error("总结文章失败: url={}", articleUrl, e);
            return "总结失败：" + e.getMessage();
        }
    }

    /**
     * 获取文章内容
     */
    private String fetchArticleContent(String url) {
        try {
            log.info("开始抓取文章内容: {}", url);
            
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .timeout(15000)
                    .get();

            // 移除不需要的元素
            doc.select("script, style, nav, header, footer, .advertisement, .ad, .sidebar").remove();
            
            // 尝试提取主要内容
            String content = "";
            
            // 常见的文章内容选择器
            String[] contentSelectors = {
                "article",
                ".article-content", 
                ".post-content",
                ".entry-content",
                ".content",
                "main",
                ".main-content",
                ".article-body"
            };
            
            for (String selector : contentSelectors) {
                if (doc.select(selector).size() > 0) {
                    content = doc.select(selector).first().text();
                    break;
                }
            }
            
            // 如果没有找到特定选择器，尝试提取body内容
            if (content.isEmpty()) {
                content = doc.body().text();
            }
            
            log.info("成功抓取文章内容，长度: {} 字符", content.length());
            return content;
            
        } catch (Exception e) {
            log.error("抓取文章内容失败: {}", url, e);
            return null;
        }
    }

    /**
     * 截断内容到指定长度
     */
    private String truncateContent(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "\n\n[内容已截断...]";
    }

    /**
     * 调用阿里云百炼大模型 HTTP API
     */
    private String callAlibabaAI(String model, String prompt) {
        try {
            // 检查API Key
            if ("your-api-key-here".equals(apiKey)) {
                return "请先配置阿里云百炼大模型的API Key";
            }

            String url = baseUrl + "/api/v1/services/aigc/text-generation/generation";
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            Map<String, Object> input = new HashMap<>();
            input.put("prompt", prompt);
            requestBody.put("input", input);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("max_tokens", 2000);
            parameters.put("temperature", 0.7);
            requestBody.put("parameters", parameters);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("X-DashScope-SSE", "disable");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("调用阿里云AI API: model={}, url={}", model, url);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                if (jsonResponse.has("output") && jsonResponse.get("output").has("text")) {
                    return jsonResponse.get("output").get("text").asText();
                } else {
                    log.error("AI API响应格式异常: {}", response.getBody());
                    return "AI响应格式异常";
                }
            } else {
                log.error("AI API调用失败: status={}, body={}", response.getStatusCode(), response.getBody());
                return "AI API调用失败：" + response.getStatusCode();
            }
            
        } catch (Exception e) {
            log.error("调用阿里云AI失败", e);
            return "AI调用异常：" + e.getMessage();
        }
    }
} 