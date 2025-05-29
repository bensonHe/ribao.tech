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
    
    private static final String BASE_URL = "https://github.com";
    private static final String TRENDING_URL = "https://github.com/trending";
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    };
    
    private final Random random = new Random();
    
    @Override
    public String getName() {
        return "GitHub Trending Crawler";
    }
    
    @Override
    public String getSource() {
        return "GitHub Trending";
    }
    
    @Override
    public List<Article> crawlArticles(int limit) {
        List<Article> articles = new ArrayList<>();
        
        try {
            log.info("🌐 开始爬取 GitHub 热门项目，目标数量: {}", Math.min(limit, 3));
            
            // 只获取前3个最热门的项目
            articles.addAll(crawlTrendingRepositories(3));
            
            log.info("✅ GitHub 热门项目爬取完成，成功获取 {} 个项目", articles.size());
            
        } catch (Exception e) {
            log.error("❌ GitHub 热门项目爬取失败", e);
        }
        
        return articles;
    }
    
    /**
     * 爬取热门仓库
     */
    private List<Article> crawlTrendingRepositories(int limit) {
        List<Article> articles = new ArrayList<>();
        
        try {
            log.info("📄 爬取 GitHub 今日热门项目: {}", TRENDING_URL);
            
            Document doc = Jsoup.connect(TRENDING_URL)
                    .userAgent(getRandomUserAgent())
                    .timeout(15000)
                    .followRedirects(true)
                    .referrer("https://github.com")
                    .get();
            
            // GitHub trending 页面的仓库列表
            Elements repoElements = doc.select("article.Box-row, .Box-row");
            
            int count = 0;
            for (Element element : repoElements) {
                if (count >= limit) break;
                
                try {
                    Article article = parseGitHubRepository(element);
                    if (article != null) {
                        articles.add(article);
                        count++;
                        log.debug("📝 解析 GitHub 项目: {}", article.getTitle());
                    }
                    
                    randomDelay();
                } catch (Exception e) {
                    log.warn("解析 GitHub 项目失败", e);
                }
            }
            
        } catch (Exception e) {
            log.error("爬取 GitHub 热门项目失败", e);
        }
        
        return articles;
    }
    
    /**
     * 解析GitHub仓库信息
     */
    private Article parseGitHubRepository(Element element) {
        try {
            // 获取仓库名称和链接
            Element titleElement = element.select("h2 a, .h3 a").first();
            if (titleElement == null) return null;
            
            String repoName = titleElement.text().trim().replace("\n", "").replaceAll("\\s+", " ");
            String repoUrl = titleElement.absUrl("href");
            
            if (repoName.isEmpty() || repoUrl.isEmpty()) return null;
            
            // 获取描述
            Element descElement = element.select("p, .color-fg-muted").first();
            String description = descElement != null ? descElement.text().trim() : "";
            
            // 获取编程语言
            Element langElement = element.select("[itemprop='programmingLanguage']").first();
            String language = langElement != null ? langElement.text().trim() : "Unknown";
            
            // 获取星标数
            Element starsElement = element.select("a[href*='/stargazers']").first();
            String starsText = starsElement != null ? starsElement.text().trim() : "0";
            int stars = parseStarCount(starsText);
            
            // 获取今日星标增长
            Element todayStarsElement = element.select(".d-inline-block.float-sm-right").first();
            String todayStarsText = todayStarsElement != null ? todayStarsElement.text().trim() : "";
            
            // 构建标题
            String title = String.format("🔥 GitHub热门: %s", repoName);
            
            // 构建摘要
            StringBuilder summary = new StringBuilder();
            summary.append(description.isEmpty() ? "GitHub热门开源项目" : description);
            if (!todayStarsText.isEmpty()) {
                summary.append(" | 今日新增: ").append(todayStarsText);
            }
            summary.append(" | ⭐ ").append(formatStarCount(stars));
            
            // 构建标签
            String tags = String.format("GitHub,开源,%s,热门项目", language);
            
            Article article = new Article();
            article.setTitle(title);
            article.setUrl(repoUrl);
            article.setSummary(summary.toString());
            article.setSource(getSource());
            article.setAuthor(extractOwnerFromUrl(repoUrl));
            article.setTags(tags);
            article.setPublishTime(LocalDateTime.now());
            article.setCreatedAt(LocalDateTime.now());
            article.setViews(stars / 10 + random.nextInt(500)); // 基于星标数估算浏览量
            article.setLikes(stars / 100 + random.nextInt(50));
            
            return article;
            
        } catch (Exception e) {
            log.warn("解析 GitHub 仓库失败", e);
            return null;
        }
    }
    
    /**
     * 解析星标数（处理k格式）
     */
    private int parseStarCount(String starsText) {
        try {
            starsText = starsText.replaceAll("[^0-9k.]", "").toLowerCase();
            if (starsText.contains("k")) {
                float num = Float.parseFloat(starsText.replace("k", ""));
                return (int) (num * 1000);
            } else {
                return starsText.isEmpty() ? 0 : Integer.parseInt(starsText);
            }
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 格式化星标数显示
     */
    private String formatStarCount(int stars) {
        if (stars >= 1000) {
            return String.format("%.1fk", stars / 1000.0);
        }
        return String.valueOf(stars);
    }
    
    /**
     * 从URL提取仓库所有者
     */
    private String extractOwnerFromUrl(String url) {
        try {
            String[] parts = url.split("/");
            if (parts.length >= 4) {
                return parts[3]; // github.com/owner/repo
            }
        } catch (Exception e) {
            // ignore
        }
        return "GitHub User";
    }
    
    /**
     * 获取随机User-Agent
     */
    private String getRandomUserAgent() {
        return USER_AGENTS[random.nextInt(USER_AGENTS.length)];
    }
    
    /**
     * 随机延迟
     */
    private void randomDelay() {
        try {
            // 2-4秒随机延迟，GitHub相对宽松
            long delay = 2000 + random.nextInt(2000);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("延迟被中断", e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        try {
            Document doc = Jsoup.connect("https://github.com")
                    .userAgent(getRandomUserAgent())
                    .timeout(10000)
                    .get();
            
            return doc.title().contains("GitHub") || doc.title().length() > 0;
            
        } catch (Exception e) {
            log.warn("GitHub 可用性检查失败", e);
            return false;
        }
    }
} 