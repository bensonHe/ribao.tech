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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Slf4j
public class DevToCrawler implements WebCrawler {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();
    
    private static final String DEV_TO_API_URL = "https://dev.to/api/articles";
    
    @Override
    public String getName() {
        return "Dev.to Crawler";
    }
    
    @Override
    public String getSource() {
        return "Dev.to";
    }
    
    @Override
    public List<Article> crawlArticles(int limit) {
        List<Article> articles = new ArrayList<>();
        
        try {
            log.info("开始爬取 Dev.to 热门文章...");
            
            String url = DEV_TO_API_URL + "?per_page=" + limit + "&top=7";
            
            // 设置请求头，模拟真实浏览器
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7");
            headers.set("Cache-Control", "no-cache");
            headers.set("Pragma", "no-cache");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String response = responseEntity.getBody();
            JsonNode articlesArray = objectMapper.readTree(response);
            
            for (JsonNode articleNode : articlesArray) {
                try {
                    Article article = parseDevToArticle(articleNode);
                    if (article != null) {
                        articles.add(article);
                    }
                } catch (Exception e) {
                    log.warn("解析 Dev.to 文章失败: {}", e.getMessage());
                }
            }
            
            log.info("成功爬取 {} 篇 Dev.to 文章", articles.size());
            
        } catch (Exception e) {
            log.error("爬取 Dev.to 失败", e);
        }
        
        return articles;
    }
    
    private Article parseDevToArticle(JsonNode articleData) {
        try {
            Article article = new Article();
            
            article.setTitle(articleData.get("title").asText());
            article.setUrl(articleData.get("url").asText());
            article.setSummary(articleData.get("description").asText());
            article.setSource(getSource());
            
            // 获取作者信息
            JsonNode userNode = articleData.get("user");
            if (userNode != null) {
                article.setAuthor(userNode.get("name").asText(""));
            }
            
            // 获取标签
            JsonNode tagsArray = articleData.get("tag_list");
            if (tagsArray != null && tagsArray.isArray()) {
                StringBuilder tags = new StringBuilder();
                for (JsonNode tag : tagsArray) {
                    if (tags.length() > 0) tags.append(",");
                    tags.append(tag.asText());
                }
                article.setTags(tags.toString());
            }
            
            // 解析发布时间
            String publishedAt = articleData.get("published_at").asText();
            if (publishedAt != null && !publishedAt.isEmpty()) {
                try {
                    LocalDateTime publishTime = LocalDateTime.parse(
                        publishedAt.replace("Z", ""), 
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    );
                    article.setPublishTime(publishTime);
                } catch (Exception e) {
                    article.setPublishTime(LocalDateTime.now());
                }
            }
            
            // 设置统计数据
            article.setViews(articleData.has("page_views_count") ? 
                articleData.get("page_views_count").asInt() : random.nextInt(1000));
            article.setLikes(articleData.has("public_reactions_count") ? 
                articleData.get("public_reactions_count").asInt() : random.nextInt(100));
            article.setStatus(Article.ArticleStatus.PUBLISHED);
            
            return article;
            
        } catch (Exception e) {
            log.warn("解析 Dev.to 文章数据失败: {}", e.getMessage());
            return null;
        }
    }
}