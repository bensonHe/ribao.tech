package com.spideman.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 推荐文章DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedArticle {
    
    /**
     * 文章标题
     */
    private String title;
    
    /**
     * 文章链接
     */
    private String url;
    
    /**
     * 文章内容简介
     */
    private String summary;
    
    /**
     * 文章推荐理由
     */
    private String reason;
    
    /**
     * 文章来源
     */
    private String source;
    
    /**
     * 文章作者
     */
    private String author;
} 