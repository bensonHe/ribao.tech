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
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();
    
    private static final String BASE_URL = "https://hacker-news.firebaseio.com/v0";
    private static final String TOP_STORIES_URL = BASE_URL + "/topstories.json";
    private static final String ITEM_URL = BASE_URL + "/item/%d.json";
    
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
        
        try {
            log.info("开始爬取 Hacker News 热门文章...");
            
            // 获取热门文章ID列表
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(TOP_STORIES_URL, HttpMethod.GET, entity, String.class);
            String response = responseEntity.getBody();
            JsonNode storyIds = objectMapper.readTree(response);
            
            int count = 0;
            for (JsonNode storyIdNode : storyIds) {
                if (count >= limit) break;
                
                try {
                    long storyId = storyIdNode.asLong();
                    Article article = fetchStoryDetails(storyId);
                    
                    if (article != null && article.getUrl() != null && !article.getUrl().isEmpty()) {
                        articles.add(article);
                        count++;
                        
                        // 添加随机延迟，避免被限制
                        Thread.sleep(500 + random.nextInt(1000));
                    }
                    
                } catch (Exception e) {
                    log.warn("获取 Hacker News 文章详情失败: {}", e.getMessage());
                }
            }
            
            log.info("成功爬取 {} 篇 Hacker News 文章", articles.size());
            
        } catch (Exception e) {
            log.error("爬取 Hacker News 失败", e);
        }
        
        return articles;
    }
    
    private Article fetchStoryDetails(long storyId) {
        try {
            String url = String.format(ITEM_URL, storyId);
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String response = responseEntity.getBody();
            JsonNode storyData = objectMapper.readTree(response);
            
            if (storyData == null || !storyData.has("url")) {
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
            
            return article;
            
        } catch (Exception e) {
            log.warn("解析 Hacker News 文章失败: storyId={}, error={}", storyId, e.getMessage());
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