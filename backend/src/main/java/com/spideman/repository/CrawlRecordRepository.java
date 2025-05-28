package com.spideman.repository;

import com.spideman.entity.CrawlRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CrawlRecordRepository extends JpaRepository<CrawlRecord, Long> {
    
    /**
     * 根据来源查找最近的爬取记录
     */
    List<CrawlRecord> findBySourceOrderByCrawlTimeDesc(String source);
    
    /**
     * 查找今日的爬取记录
     */
    @Query("SELECT cr FROM CrawlRecord cr WHERE DATE(cr.crawlTime) = DATE(CURRENT_DATE)")
    List<CrawlRecord> findTodayRecords();
    
    /**
     * 按状态查找记录
     */
    List<CrawlRecord> findByStatusOrderByCrawlTimeDesc(CrawlRecord.CrawlStatus status);
    
    /**
     * 统计各来源的爬取次数
     */
    @Query("SELECT cr.source, COUNT(cr), SUM(cr.successCount), SUM(cr.errorCount) " +
           "FROM CrawlRecord cr " +
           "GROUP BY cr.source " +
           "ORDER BY COUNT(cr) DESC")
    List<Object[]> getSourceStatistics();
} 