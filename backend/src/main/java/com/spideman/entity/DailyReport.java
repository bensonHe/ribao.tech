package com.spideman.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private LocalDate reportDate;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String summary; // 日报摘要
    
    @Column(columnDefinition = "TEXT")
    private String content; // 日报内容
    
    @Column(columnDefinition = "TEXT")
    private String highlights; // 今日亮点
    
    @Column(columnDefinition = "TEXT")
    private String trends; // 技术趋势
    
    @Column(columnDefinition = "TEXT")
    private String articleIds; // 关联的文章ID，逗号分隔
    
    private Integer totalArticles = 0; // 总文章数
    private Integer readCount = 0; // 阅读次数
    
    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.DRAFT;
    
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.generatedAt == null) {
            this.generatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum ReportStatus {
        DRAFT,        // 草稿
        GENERATING,   // 生成中
        PUBLISHED,    // 已发布
        ARCHIVED      // 已归档
    }
} 