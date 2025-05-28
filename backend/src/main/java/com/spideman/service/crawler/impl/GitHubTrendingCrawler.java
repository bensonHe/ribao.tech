package com.spideman.service.crawler.impl;

import com.spideman.entity.Article;
import com.spideman.service.crawler.WebCrawler;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Slf4j
public class GitHubTrendingCrawler implements WebCrawler {
    
    private final Random random = new Random();
    private static final String GITHUB_TRENDING_URL = "https://github.com/trending";
    
    @Override
    public String getName() {
        return "GitHub Trending Crawler";
    }
    
    @Override
    public String getSource() {
        return "GitHub";
    }
    
    @Override
    public List<Article> crawlArticles(int limit) {
        List<Article> articles = new ArrayList<>();
        
        try {
            log.info("开始爬取 GitHub Trending 项目...");
            
            Document doc = Jsoup.connect(GITHUB_TRENDING_URL)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();
            
            Elements repos = doc.select("article.Box-row");
            
            int count = 0;
            for (Element repo : repos) {
                if (count >= limit) break;
                
                try {
                    Article article = parseRepository(repo);
                    if (article != null) {
                        articles.add(article);
                        count++;
                    }
                } catch (Exception e) {
                    log.warn("解析 GitHub 项目失败: {}", e.getMessage());
                }
            }
            
            log.info("成功爬取 {} 个 GitHub Trending 项目", articles.size());
            
        } catch (Exception e) {
            log.error("爬取 GitHub Trending 失败", e);
        }
        
        return articles;
    }
    
    private Article parseRepository(Element repo) {
        try {
            // 项目名称和链接
            Element titleElement = repo.selectFirst("h2.h3 a");
            if (titleElement == null) return null;
            
            String repoName = cleanText(titleElement.text());
            String repoUrl = "https://github.com" + titleElement.attr("href");
            
            // 描述
            Element descElement = repo.selectFirst("p.col-9");
            String description = descElement != null ? cleanText(descElement.text()) : "";
            
            // Star 数
            Element starsElement = repo.selectFirst("a[href*='stargazers']");
            String starsText = starsElement != null ? cleanText(starsElement.text()) : "0";
            int stars = parseStars(starsText);
            
            // 编程语言
            Element languageElement = repo.selectFirst("span[itemprop='programmingLanguage']");
            String language = languageElement != null ? cleanText(languageElement.text()) : "Unknown";
            
            // 创建文章对象
            Article article = new Article();
            article.setTitle(repoName + " - GitHub Trending 项目");
            article.setUrl(repoUrl);
            article.setSummary(description.isEmpty() ? 
                String.format("GitHub 热门 %s 项目，Star 数：%d", language, stars) : description);
            article.setSource(getSource());
            article.setAuthor(repoName.contains("/") ? repoName.split("/")[0] : "");
            article.setTags(String.format("GitHub,%s,OpenSource", language));
            article.setPublishTime(LocalDateTime.now());
            article.setViews(stars);
            article.setLikes(random.nextInt(stars / 10 + 1));
            article.setStatus(Article.ArticleStatus.PUBLISHED);
            
            return article;
            
        } catch (Exception e) {
            log.warn("解析 GitHub 项目详情失败: {}", e.getMessage());
            return null;
        }
    }
    
    private String cleanText(String text) {
        return text != null ? text.trim().replaceAll("\\s+", " ") : "";
    }
    
    private int parseStars(String starsText) {
        try {
            // 移除所有非数字字符
            String numericText = starsText.replaceAll("[^0-9]", "");
            return numericText.isEmpty() ? 0 : Integer.parseInt(numericText);
        } catch (Exception e) {
            return 0;
        }
    }
} 