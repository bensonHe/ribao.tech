package com.spideman.repository;

import com.spideman.entity.DailyReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {
    
    // 根据日期查找报告
    Optional<DailyReport> findByReportDate(LocalDate reportDate);
    
    // 根据状态查找报告
    List<DailyReport> findByStatusOrderByReportDateDesc(DailyReport.ReportStatus status);
    
    // 分页查询报告
    Page<DailyReport> findByStatusOrderByReportDateDesc(DailyReport.ReportStatus status, Pageable pageable);
    
    // 获取最新的报告
    Optional<DailyReport> findFirstByStatusOrderByReportDateDesc(DailyReport.ReportStatus status);
    
    // 获取最近N天的报告
    List<DailyReport> findByReportDateAfterOrderByReportDateDesc(LocalDate date);
} 