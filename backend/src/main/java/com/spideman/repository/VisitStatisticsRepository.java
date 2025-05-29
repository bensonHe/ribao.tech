package com.spideman.repository;

import com.spideman.entity.VisitStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisitStatisticsRepository extends JpaRepository<VisitStatistics, Long> {
    
    /**
     * 根据日期查找访问统计
     */
    Optional<VisitStatistics> findByVisitDate(LocalDate visitDate);
    
    /**
     * 获取指定日期范围内的访问统计
     */
    @Query("SELECT v FROM VisitStatistics v WHERE v.visitDate BETWEEN :startDate AND :endDate ORDER BY v.visitDate DESC")
    List<VisitStatistics> findByVisitDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * 获取最近N天的访问统计
     */
    @Query("SELECT v FROM VisitStatistics v ORDER BY v.visitDate DESC")
    List<VisitStatistics> findRecentStatistics();
    
    /**
     * 获取总访问量
     */
    @Query("SELECT COALESCE(SUM(v.pageViews), 0) FROM VisitStatistics v")
    Long getTotalPageViews();
    
    /**
     * 获取总独立访客数
     */
    @Query("SELECT COALESCE(SUM(v.uniqueVisitors), 0) FROM VisitStatistics v")
    Long getTotalUniqueVisitors();
    
    /**
     * 获取最近7天的统计数据
     */
    @Query("SELECT v FROM VisitStatistics v WHERE v.visitDate >= :startDate ORDER BY v.visitDate DESC")
    List<VisitStatistics> findLast7Days(@Param("startDate") LocalDate startDate);
    
    /**
     * 获取最近30天的统计数据
     */
    @Query("SELECT v FROM VisitStatistics v WHERE v.visitDate >= :startDate ORDER BY v.visitDate DESC")
    List<VisitStatistics> findLast30Days(@Param("startDate") LocalDate startDate);
} 