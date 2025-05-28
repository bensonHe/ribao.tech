package com.spideman.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrawlRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String source;
    
    @Column(name = "total_crawled")
    private Integer totalCrawled = 0;
    
    @Column(name = "success_count")
    private Integer successCount = 0;
    
    @Column(name = "error_count")
    private Integer errorCount = 0;
    
    @Column(name = "crawl_time")
    private LocalDateTime crawlTime = LocalDateTime.now();
    
    @Enumerated(EnumType.STRING)
    private CrawlStatus status = CrawlStatus.RUNNING;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    public enum CrawlStatus {
        RUNNING, COMPLETED, FAILED
    }
} 