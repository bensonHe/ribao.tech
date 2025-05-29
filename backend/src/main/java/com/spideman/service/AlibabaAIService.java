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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
            String dayOfWeek = java.time.LocalDate.now().getDayOfWeek().getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale.CHINESE);
            
            // 获取节气信息
            String solarTerm = getSolarTerm(java.time.LocalDate.now());
            List<String> listNames = Arrays.asList("雷军", "阮一峰","马斯克", "马云", "李彦宏", "周鸿祎","张小龙", "马化腾", "马云", "李彦宏", "周鸿祎", "王小川");
            
            String prompt = String.format(
                "请以["+listNames.get(new Random().nextInt(listNames.size()))+"]的口吻，写一篇适合发布在公众号或技术网站上的 AI 行业日报。\n" +
                "请根据以下内容，生成一段自然、接近人类撰写风格的文字：\n" +
                "- 内容需要包含主观感受、评价词，不要机械列举项目；\n" +
                "- 文风偏“程序员口吻”，可以稍带轻松幽默，但要专业；\n" +
                "- 每条内容聚焦1-2个重点，用小标题或列表更清晰； \n" +
                "- 最终输出一段可以直接用于公众号或网站的中文内容。\n" +
                "文章列表：\n%s\n\n" +
                "请严格按照以下JSON格式返回日报内容：\n\n" +
                "{\n" +
                "  \"todayTrends\": \"[今日总结：用400字以内总结今日技术文章中的主要趋势、热点技术、重要动态等, 开头以今天我觉得值得看的一些东西有 或者 我觉得最值得关注的几个点是 或者 最近技术圈比较关注的点在 来开头, 结尾,以你们感觉怎么样 来结尾]\",\n" +
                "  \"recommendedArticles\": [\n" +
                "    {\n" +
                "      \"title\": \"[文章标题]\",\n" +
                "      \"url\": \"[文章链接]\",\n" +
                "      \"summary\": \"[文章简介：用200字左右简要介绍文章核心内容]\",\n" +
                "      \"reason\": \"[推荐理由：用1句话说明为什么推荐这篇文章，例如: 开头以 我觉得这个 或 我发现 或 这个文章值得关注点在于 来开头, 结尾以你们感觉怎么样 或者 值得看一看 或者 可以学习下 来结尾]\",\n" +
                "      \"source\": \"[文章来源]\",\n" +
                "      \"author\": \"[文章作者]\"\n" +
                "    }\n" +
                "    // 请从上述文章中选择3-5篇最有价值的文章\n" +
                "  ],\n" +
                "  \"dailyQuote\": \"[每日一语：结合今日日期（%s，%s）和当前节气（%s），写一句100字以内的鼓励文或名言。目的是新的一天开始、减少焦虑感、鼓励程序员。可以结合技术成长、学习心态、工作生活平衡等主题, 需要带上打工人看开了的语气]\",\n" +
                "  \"solarTerm\": \"%s\"\n" +
                "}\n\n" +
                "注意：\n" +
                "1. 今日总结必须控制在400字以内\n" +
                "2. 每日一语必须控制在100字左右\n" +
                "3. 推荐文章要选择最有价值的3-5篇\n" +
                "4. 推荐理由要简洁明了，一句话即可\n" +
                "5. 返回的必须是有效的JSON格式\n" +
                "6. 文章链接必须使用原文章的真实URL\n" +
                "7. 不要在reason字段中重复'推荐理由'这个词\n",
                articlesText.toString(),
                currentDate, dayOfWeek, solarTerm,
                solarTerm
            );

            log.info("📝 AI提示词统计:");
            log.info("   - 提示词长度: {} 字符", prompt.length());
            log.info("   - 目标模型: qwen-plus-latest");
            log.info("   - 预期输出: Markdown格式日报");
            log.info("   - 完整prompt为: {}", prompt);    

            long aiCallStart = System.currentTimeMillis();
            log.info("🚀 开始调用AI模型...");

            String result = callAlibabaAI("qwen-plus-latest", prompt);
            
            long aiCallEnd = System.currentTimeMillis();
            long aiCallDuration = aiCallEnd - aiCallStart;
            long totalDuration = aiCallEnd - startTime;

            log.info("🎯 AI调用完成:");
            log.info("   - AI调用耗时: {} ms", aiCallDuration);
            log.info("   - 总处理耗时: {} ms", totalDuration);
            log.info("   - 返回内容长度: {} 字符", result != null ? result.length() : 0);
            log.info("   - 完整返回内容预览: {}", result);  

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
    
    /**
     * 获取当前节气
     */
    private String getSolarTerm(java.time.LocalDate date) {
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
} 