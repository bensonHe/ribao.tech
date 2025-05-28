package com.spideman.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Article {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(length = 500)
    private String titleZh; // 中文标题
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(columnDefinition = "TEXT")
    private String contentZh; // 中文内容
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(columnDefinition = "TEXT")
    private String summaryZh; // 中文摘要
    
    @Column(nullable = false, unique = true, length = 1000)
    private String url;
    
    @Column(length = 200)
    private String source; // 来源网站
    
    @Column(length = 200)
    private String author;
    
    @Column(name = "publish_time")
    private LocalDateTime publishTime;
    
    @Column(name = "crawl_time")
    private LocalDateTime crawlTime;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    private Integer likes = 0;
    private Integer views = 0;
    
    @Column(length = 500)
    private String tags; // 标签，逗号分隔
    
    @Enumerated(EnumType.STRING)
    private ArticleStatus status = ArticleStatus.PENDING;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.crawlTime == null) {
            this.crawlTime = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum ArticleStatus {
        PENDING,      // 待处理
        TRANSLATED,   // 已翻译
        PUBLISHED,    // 已发布
        ARCHIVED      // 已归档
    }
} 