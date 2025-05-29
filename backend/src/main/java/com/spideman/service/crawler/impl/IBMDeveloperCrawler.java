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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Slf4j
public class IBMDeveloperCrawler implements WebCrawler {
    
    private static final String BASE_URL = "https://developer.ibm.com";
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    };
    
    private final Random random = new Random();
    
    @Override
    public String getName() {
        return "IBM Developer Crawler";
    }
    
    @Override
    public String getSource() {
        return "IBM Developer";
    }
    
    @Override
    public List<Article> crawlArticles(int limit) {
        List<Article> articles = new ArrayList<>();
        
        try {
            log.info("🌐 开始爬取 IBM Developer 文章，目标数量: {}", Math.min(limit, 3));
            
            // 爬取不同类型的文章
            articles.addAll(crawlArticlesByCategory("articles", 2)); // 技术文章
            randomDelay();
            articles.addAll(crawlArticlesByCategory("tutorials", 1)); // 教程
            
            // 确保只返回前3篇
            if (articles.size() > 3) {
                articles = articles.subList(0, 3);
            }
            
            log.info("✅ IBM Developer 爬取完成，成功获取 {} 篇文章", articles.size());
            
        } catch (Exception e) {
            log.error("❌ IBM Developer 爬取失败", e);
        }
        
        return articles;
    }
    
    /**
     * 按分类爬取文章
     */
    private List<Article> crawlArticlesByCategory(String category, int limit) {
        List<Article> articles = new ArrayList<>();
        
        try {
            String url = String.format("%s/%s", BASE_URL, category);
            log.info("📄 爬取 IBM Developer 分类: {} - {}", category, url);
            
            Document doc = Jsoup.connect(url)
                    .userAgent(getRandomUserAgent())
                    .timeout(15000)
                    .followRedirects(true)
                    .referrer("https://developer.ibm.com")
                    .get();
            
            // IBM Developer 文章列表选择器
            Elements articleElements = doc.select("article, .card, .content-item, .article-card");
            
            // 如果没找到，尝试其他选择器
            if (articleElements.isEmpty()) {
                articleElements = doc.select(".bx--tile, .bx--card, .ibm-card");
            }
            
            int count = 0;
            for (Element element : articleElements) {
                if (count >= limit) break;
                
                try {
                    Article article = parseIBMDeveloperArticle(element, category);
                    if (article != null) {
                        articles.add(article);
                        count++;
                        log.debug("📝 解析 IBM Developer 文章: {}", article.getTitle());
                    }
                    
                    randomDelay();
                } catch (Exception e) {
                    log.warn("解析 IBM Developer 文章失败", e);
                }
            }
            
            // 如果没有找到文章，尝试备用方法
            if (articles.isEmpty()) {
                articles.addAll(crawlAlternativeMethod(limit));
            }
            
        } catch (Exception e) {
            log.error("爬取 IBM Developer 分类失败: {}", category, e);
        }
        
        return articles;
    }
    
    /**
     * 备用方法：爬取IBM博客
     */
    private List<Article> crawlAlternativeMethod(int limit) {
        List<Article> articles = new ArrayList<>();
        
        try {
            log.info("📄 使用备用方法爬取 IBM 技术博客");
            
            String url = "https://www.ibm.com/blog/";
            Document doc = Jsoup.connect(url)
                    .userAgent(getRandomUserAgent())
                    .timeout(15000)
                    .get();
            
            Elements articleElements = doc.select("article, .post, .blog-post");
            
            int count = 0;
            for (Element element : articleElements) {
                if (count >= limit) break;
                
                try {
                    Element titleElement = element.select("h2 a, h3 a, .title a").first();
                    if (titleElement == null) continue;
                    
                    String title = titleElement.text().trim();
                    String articleUrl = titleElement.absUrl("href");
                    
                    if (title.isEmpty() || articleUrl.isEmpty()) continue;
                    
                    // 获取摘要
                    Element summaryElement = element.select(".excerpt, .summary, p").first();
                    String summary = summaryElement != null ? summaryElement.text().trim() : "";
                    
                    // 获取作者
                    Element authorElement = element.select(".author, .by-author").first();
                    String author = authorElement != null ? authorElement.text().trim() : "IBM";
                    
                    Article article = new Article();
                    article.setTitle("💼 IBM技术: " + title);
                    article.setUrl(articleUrl);
                    article.setSummary(summary.isEmpty() ? "IBM 企业级技术文章" : summary);
                    article.setSource(getSource());
                    article.setAuthor(author);
                    article.setTags("IBM,企业技术,云计算,AI,数据科学");
                    article.setPublishTime(LocalDateTime.now().minusHours(random.nextInt(72)));
                    article.setCreatedAt(LocalDateTime.now());
                    article.setViews(random.nextInt(800) + 200);
                    article.setLikes(random.nextInt(40) + 10);
                    
                    articles.add(article);
                    count++;
                    
                } catch (Exception e) {
                    log.warn("解析 IBM 博客文章失败", e);
                }
            }
            
        } catch (Exception e) {
            log.error("备用爬取方法失败", e);
        }
        
        return articles;
    }
    
    /**
     * 解析IBM Developer文章
     */
    private Article parseIBMDeveloperArticle(Element element, String category) {
        try {
            // 获取标题和链接
            Element titleElement = element.select("h2 a, h3 a, .title a, .card-title a").first();
            if (titleElement == null) {
                titleElement = element.select("a").first();
            }
            if (titleElement == null) return null;
            
            String title = titleElement.text().trim();
            String articleUrl = titleElement.absUrl("href");
            
            if (title.isEmpty() || articleUrl.isEmpty()) return null;
            
            // 获取摘要
            Element summaryElement = element.select(".summary, .excerpt, .description, p").first();
            String summary = summaryElement != null ? summaryElement.text().trim() : "";
            
            // 获取作者
            Element authorElement = element.select(".author, .by-author, .contributor").first();
            String author = authorElement != null ? authorElement.text().trim() : "IBM Developer";
            
            // 获取发布时间
            Element dateElement = element.select(".date, .published, time").first();
            String dateText = dateElement != null ? dateElement.text().trim() : "";
            
            // 根据分类构建标题前缀
            String titlePrefix = "💼 IBM技术: ";
            if ("tutorials".equals(category)) {
                titlePrefix = "📚 IBM教程: ";
            }
            
            // 构建标签
            String tags = String.format("IBM,企业技术,%s,开发者", category);
            if (title.toLowerCase().contains("ai") || title.toLowerCase().contains("watson")) {
                tags += ",AI,Watson";
            }
            if (title.toLowerCase().contains("cloud") || title.toLowerCase().contains("kubernetes")) {
                tags += ",云计算,Kubernetes";
            }
            if (title.toLowerCase().contains("data") || title.toLowerCase().contains("analytics")) {
                tags += ",数据科学,分析";
            }
            
            Article article = new Article();
            article.setTitle(titlePrefix + title);
            article.setUrl(articleUrl);
            article.setSummary(summary.isEmpty() ? "IBM Developer 企业级技术内容" : summary);
            article.setSource(getSource());
            article.setAuthor(author);
            article.setTags(tags);
            article.setPublishTime(LocalDateTime.now().minusHours(random.nextInt(168))); // 一周内
            article.setCreatedAt(LocalDateTime.now());
            article.setViews(random.nextInt(1200) + 300);
            article.setLikes(random.nextInt(60) + 15);
            
            return article;
            
        } catch (Exception e) {
            log.warn("解析 IBM Developer 文章失败", e);
            return null;
        }
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
            // 3-5秒随机延迟，IBM网站相对稳定但需要适当延迟
            long delay = 3000 + random.nextInt(2000);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("延迟被中断", e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        try {
            Document doc = Jsoup.connect("https://developer.ibm.com")
                    .userAgent(getRandomUserAgent())
                    .timeout(10000)
                    .get();
            
            return doc.title().contains("IBM") || doc.title().length() > 0;
            
        } catch (Exception e) {
            log.warn("IBM Developer 可用性检查失败", e);
            return false;
        }
    }
} 