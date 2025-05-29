package com.spideman.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 访问统计实体
 */
@Entity
@Table(name = "visit_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 统计日期
     */
    @Column(name = "visit_date", nullable = false, unique = true)
    private LocalDate visitDate;
    
    /**
     * 页面访问量（PV）
     */
    @Column(name = "page_views", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long pageViews = 0L;
    
    /**
     * 独立访客数（UV）
     */
    @Column(name = "unique_visitors", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long uniqueVisitors = 0L;
    
    /**
     * 首页访问量
     */
    @Column(name = "home_visits", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long homeVisits = 0L;
    
    /**
     * 日报详情页访问量
     */
    @Column(name = "report_visits", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long reportVisits = 0L;
    
    /**
     * 文章详情页访问量
     */
    @Column(name = "article_visits", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long articleVisits = 0L;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 构造函数
     */
    public VisitStatistics(LocalDate visitDate) {
        this.visitDate = visitDate;
        this.pageViews = 0L;
        this.uniqueVisitors = 0L;
        this.homeVisits = 0L;
        this.reportVisits = 0L;
        this.articleVisits = 0L;
    }
} 