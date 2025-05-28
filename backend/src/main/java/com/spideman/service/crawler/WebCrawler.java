package com.spideman.service.crawler;

import com.spideman.entity.Article;
import java.util.List;

/**
 * 网络爬虫接口
 */
public interface WebCrawler {
    
    /**
     * 获取爬虫名称
     */
    String getName();
    
    /**
     * 获取来源网站
     */
    String getSource();
    
    /**
     * 爬取文章
     */
    List<Article> crawlArticles(int limit);
    
    /**
     * 检查是否可用
     */
    default boolean isAvailable() {
        return true;
    }
} 