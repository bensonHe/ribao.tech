package com.spideman.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 访问记录实体
 */
@Entity
@Table(name = "visit_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 访客IP地址
     */
    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;
    
    /**
     * 用户代理字符串
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * 访问的页面路径
     */
    @Column(name = "page_path", nullable = false, length = 500)
    private String pagePath;
    
    /**
     * 页面类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "page_type", nullable = false)
    private PageType pageType;
    
    /**
     * 来源页面（Referer）
     */
    @Column(name = "referer", length = 500)
    private String referer;
    
    /**
     * 访问时间
     */
    @Column(name = "visit_time", nullable = false)
    private LocalDateTime visitTime;
    
    /**
     * 会话ID（用于识别同一用户的多次访问）
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    /**
     * 页面类型枚举
     */
    public enum PageType {
        HOME("首页"),
        REPORT_DETAIL("日报详情"),
        ARTICLE_DETAIL("文章详情"),
        ARTICLE_LIST("文章列表"),
        OTHER("其他");
        
        private final String description;
        
        PageType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @PrePersist
    protected void onCreate() {
        if (visitTime == null) {
            visitTime = LocalDateTime.now();
        }
    }
    
    /**
     * 构造函数
     */
    public VisitRecord(String ipAddress, String userAgent, String pagePath, PageType pageType, String referer, String sessionId) {
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.pagePath = pagePath;
        this.pageType = pageType;
        this.referer = referer;
        this.sessionId = sessionId;
        this.visitTime = LocalDateTime.now();
    }
} 