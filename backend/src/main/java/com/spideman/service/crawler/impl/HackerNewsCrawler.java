package com.spideman.service.crawler.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spideman.entity.Article;
import com.spideman.service.crawler.WebCrawler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Slf4j
public class HackerNewsCrawler implements WebCrawler {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();
    
    private static final String BASE_URL = "https://hacker-news.firebaseio.com/v0";
    private static final String TOP_STORIES_URL = BASE_URL + "/topstories.json";
    private static final String ITEM_URL = BASE_URL + "/item/%d.json";
    
    public HackerNewsCrawler() {
        // 配置RestTemplate超时设置
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 连接超时5秒
        factory.setReadTimeout(10000);   // 读取超时10秒
        this.restTemplate = new RestTemplate(factory);
        log.info("🔧 HackerNewsCrawler 初始化完成，连接超时: 5s, 读取超时: 10s");
    }
    
    @Override
    public String getName() {
        return "Hacker News Crawler";
    }
    
    @Override
    public String getSource() {
        return "Hacker News";
    }
    
    @Override
    public List<Article> crawlArticles(int limit) {
        List<Article> articles = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("🚀 开始爬取 Hacker News 热门文章，限制数量: {}", limit);
            
            // 获取热门文章ID列表
            log.debug("📡 正在获取热门文章ID列表...");
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            long requestStart = System.currentTimeMillis();
            ResponseEntity<String> responseEntity = restTemplate.exchange(TOP_STORIES_URL, HttpMethod.GET, entity, String.class);
            long requestTime = System.currentTimeMillis() - requestStart;
            
            String response = responseEntity.getBody();
            JsonNode storyIds = objectMapper.readTree(response);
            
            log.info("✅ 获取热门文章ID列表成功，耗时: {}ms, 总数: {}", requestTime, storyIds.size());
            
            int count = 0;
            int successCount = 0;
            int failCount = 0;
            
            for (JsonNode storyIdNode : storyIds) {
                if (count >= limit) break;
                count++;
                
                try {
                    long storyId = storyIdNode.asLong();
                    log.debug("📄 正在获取文章详情: ID={}, 进度: {}/{}", storyId, count, limit);
                    
                    Article article = fetchStoryDetails(storyId);
                    
                    if (article != null && article.getUrl() != null && !article.getUrl().isEmpty()) {
                        articles.add(article);
                        successCount++;
                        log.debug("✅ 文章获取成功: {}", article.getTitle());
                        
                        // 添加随机延迟，避免被限制
                        Thread.sleep(200 + random.nextInt(300)); // 减少延迟时间
                    } else {
                        failCount++;
                        log.debug("⚠️ 文章无效或无URL: ID={}", storyId);
                    }
                    
                } catch (Exception e) {
                    failCount++;
                    log.warn("❌ 获取 Hacker News 文章详情失败: {}", e.getMessage());
                }
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("🎉 Hacker News 爬取完成！成功: {}, 失败: {}, 总耗时: {}ms", 
                successCount, failCount, totalTime);
            
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("💥 爬取 Hacker News 失败，耗时: {}ms", totalTime, e);
            
            // 返回空列表而不是抛出异常
            return new ArrayList<>();
        }
        
        return articles;
    }
    
    private Article fetchStoryDetails(long storyId) {
        try {
            String url = String.format(ITEM_URL, storyId);
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            long requestStart = System.currentTimeMillis();
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            long requestTime = System.currentTimeMillis() - requestStart;
            
            String response = responseEntity.getBody();
            JsonNode storyData = objectMapper.readTree(response);
            
            if (storyData == null || !storyData.has("url")) {
                log.debug("⚠️ 文章数据无效或缺少URL: ID={}, 耗时: {}ms", storyId, requestTime);
                return null;
            }
            
            Article article = new Article();
            article.setTitle(storyData.get("title").asText());
            article.setUrl(storyData.get("url").asText());
            article.setSummary(String.format("Hacker News 热门文章，得分：%d", 
                storyData.has("score") ? storyData.get("score").asInt() : 0));
            article.setSource(getSource());
            article.setAuthor(storyData.has("by") ? storyData.get("by").asText() : "");
            article.setTags("Tech,News,HackerNews");
            
            // 转换时间戳
            if (storyData.has("time")) {
                long timestamp = storyData.get("time").asLong();
                LocalDateTime publishTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(timestamp), 
                    ZoneId.systemDefault()
                );
                article.setPublishTime(publishTime);
            }
            
            // 设置统计数据
            article.setViews(storyData.has("score") ? storyData.get("score").asInt() : 0);
            article.setLikes(storyData.has("descendants") ? storyData.get("descendants").asInt() : 0);
            article.setStatus(Article.ArticleStatus.PUBLISHED);
            
            log.debug("✅ 文章详情获取成功: ID={}, 标题: {}, 耗时: {}ms", 
                storyId, article.getTitle(), requestTime);
            
            return article;
            
        } catch (Exception e) {
            log.warn("❌ 解析 Hacker News 文章失败: storyId={}, error={}", storyId, e.getMessage());
            return null;
        }
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Accept-Language", "en-US,en;q=0.9");
        headers.set("Cache-Control", "no-cache");
        headers.set("Pragma", "no-cache");
        return headers;
    }
} 