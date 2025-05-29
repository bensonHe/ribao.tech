package com.spideman.repository;

import com.spideman.entity.VisitRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitRecordRepository extends JpaRepository<VisitRecord, Long> {
    
    /**
     * 统计指定时间范围内的独立访客数（根据IP地址）
     */
    @Query("SELECT COUNT(DISTINCT v.ipAddress) FROM VisitRecord v WHERE v.visitTime BETWEEN :startTime AND :endTime")
    Long countUniqueVisitorsByIpBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的页面访问量
     */
    @Query("SELECT COUNT(v) FROM VisitRecord v WHERE v.visitTime BETWEEN :startTime AND :endTime")
    Long countPageViewsBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内特定页面类型的访问量
     */
    @Query("SELECT COUNT(v) FROM VisitRecord v WHERE v.visitTime BETWEEN :startTime AND :endTime AND v.pageType = :pageType")
    Long countPageViewsByTypeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("pageType") VisitRecord.PageType pageType);
    
    /**
     * 获取指定时间范围内的访问记录
     */
    @Query("SELECT v FROM VisitRecord v WHERE v.visitTime BETWEEN :startTime AND :endTime ORDER BY v.visitTime DESC")
    List<VisitRecord> findByVisitTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 检查指定IP在指定时间范围内是否已有访问记录
     */
    @Query("SELECT COUNT(v) > 0 FROM VisitRecord v WHERE v.ipAddress = :ipAddress AND v.visitTime BETWEEN :startTime AND :endTime")
    boolean existsByIpAddressAndVisitTimeBetween(@Param("ipAddress") String ipAddress, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取最近的访问记录
     */
    @Query("SELECT v FROM VisitRecord v ORDER BY v.visitTime DESC")
    List<VisitRecord> findRecentVisits();
    
    /**
     * 删除指定日期之前的访问记录（用于数据清理）
     */
    @Query("DELETE FROM VisitRecord v WHERE v.visitTime < :cutoffTime")
    void deleteOldRecords(@Param("cutoffTime") LocalDateTime cutoffTime);
} 