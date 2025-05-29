package com.spideman.service;

import com.spideman.entity.VisitRecord;
import com.spideman.entity.VisitStatistics;
import com.spideman.repository.VisitRecordRepository;
import com.spideman.repository.VisitStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitStatisticsService {
    
    private final VisitRecordRepository visitRecordRepository;
    private final VisitStatisticsRepository visitStatisticsRepository;
    
    /**
     * 记录访问
     */
    @Async
    @Transactional
    public void recordVisit(HttpServletRequest request, VisitRecord.PageType pageType) {
        try {
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String pagePath = request.getRequestURI();
            String referer = request.getHeader("Referer");
            String sessionId = request.getSession().getId();
            
            // 创建访问记录
            VisitRecord visitRecord = new VisitRecord(ipAddress, userAgent, pagePath, pageType, referer, sessionId);
            visitRecordRepository.save(visitRecord);
            
            // 更新当日统计
            updateDailyStatistics(LocalDate.now(), pageType, ipAddress);
            
            log.debug("记录访问: IP={}, 页面类型={}, 路径={}", ipAddress, pageType, pagePath);
            
        } catch (Exception e) {
            log.error("记录访问失败", e);
        }
    }
    
    /**
     * 更新当日统计数据
     */
    @Transactional
    public void updateDailyStatistics(LocalDate date, VisitRecord.PageType pageType, String ipAddress) {
        VisitStatistics statistics = visitStatisticsRepository.findByVisitDate(date)
            .orElse(new VisitStatistics(date));
        
        // 增加页面访问量
        statistics.setPageViews(statistics.getPageViews() + 1);
        
        // 根据页面类型增加对应的访问量
        switch (pageType) {
            case HOME:
                statistics.setHomeVisits(statistics.getHomeVisits() + 1);
                break;
            case REPORT_DETAIL:
                statistics.setReportVisits(statistics.getReportVisits() + 1);
                break;
            case ARTICLE_DETAIL:
                statistics.setArticleVisits(statistics.getArticleVisits() + 1);
                break;
        }
        
        // 检查是否为新的独立访客（当天首次访问）
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        if (!visitRecordRepository.existsByIpAddressAndVisitTimeBetween(ipAddress, startOfDay, endOfDay.minusSeconds(1))) {
            statistics.setUniqueVisitors(statistics.getUniqueVisitors() + 1);
        }
        
        visitStatisticsRepository.save(statistics);
    }
    
    /**
     * 获取今日统计数据
     */
    public VisitStatistics getTodayStatistics() {
        return visitStatisticsRepository.findByVisitDate(LocalDate.now())
            .orElse(new VisitStatistics(LocalDate.now()));
    }
    
    /**
     * 获取最近7天的统计数据
     */
    public List<VisitStatistics> getLast7DaysStatistics() {
        LocalDate startDate = LocalDate.now().minusDays(6);
        return visitStatisticsRepository.findLast7Days(startDate);
    }
    
    /**
     * 获取最近30天的统计数据
     */
    public List<VisitStatistics> getLast30DaysStatistics() {
        LocalDate startDate = LocalDate.now().minusDays(29);
        return visitStatisticsRepository.findLast30Days(startDate);
    }
    
    /**
     * 获取指定日期范围的统计数据
     */
    public List<VisitStatistics> getStatisticsByDateRange(LocalDate startDate, LocalDate endDate) {
        return visitStatisticsRepository.findByVisitDateBetween(startDate, endDate);
    }
    
    /**
     * 获取总体统计数据
     */
    public Map<String, Object> getOverallStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalPageViews", visitStatisticsRepository.getTotalPageViews());
        stats.put("totalUniqueVisitors", visitStatisticsRepository.getTotalUniqueVisitors());
        
        // 今日数据
        VisitStatistics todayStats = getTodayStatistics();
        stats.put("todayPageViews", todayStats.getPageViews());
        stats.put("todayUniqueVisitors", todayStats.getUniqueVisitors());
        stats.put("todayHomeVisits", todayStats.getHomeVisits());
        stats.put("todayReportVisits", todayStats.getReportVisits());
        stats.put("todayArticleVisits", todayStats.getArticleVisits());
        
        // 昨日数据（用于对比）
        VisitStatistics yesterdayStats = visitStatisticsRepository.findByVisitDate(LocalDate.now().minusDays(1))
            .orElse(new VisitStatistics(LocalDate.now().minusDays(1)));
        stats.put("yesterdayPageViews", yesterdayStats.getPageViews());
        stats.put("yesterdayUniqueVisitors", yesterdayStats.getUniqueVisitors());
        
        return stats;
    }
    
    /**
     * 获取最近访问记录
     */
    public List<VisitRecord> getRecentVisits(int limit) {
        List<VisitRecord> records = visitRecordRepository.findRecentVisits();
        return records.size() > limit ? records.subList(0, limit) : records;
    }
    
    /**
     * 定时任务：每天凌晨统计前一天的数据
     */
    @Scheduled(cron = "0 5 0 * * ?") // 每天凌晨00:05执行
    @Transactional
    public void dailyStatisticsTask() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDateTime startOfDay = yesterday.atStartOfDay();
            LocalDateTime endOfDay = yesterday.atTime(LocalTime.MAX);
            
            log.info("开始统计 {} 的访问数据", yesterday);
            
            // 获取或创建昨日统计记录
            VisitStatistics statistics = visitStatisticsRepository.findByVisitDate(yesterday)
                .orElse(new VisitStatistics(yesterday));
            
            // 重新计算统计数据（确保准确性）
            Long pageViews = visitRecordRepository.countPageViewsBetween(startOfDay, endOfDay);
            Long uniqueVisitors = visitRecordRepository.countUniqueVisitorsByIpBetween(startOfDay, endOfDay);
            Long homeVisits = visitRecordRepository.countPageViewsByTypeBetween(startOfDay, endOfDay, VisitRecord.PageType.HOME);
            Long reportVisits = visitRecordRepository.countPageViewsByTypeBetween(startOfDay, endOfDay, VisitRecord.PageType.REPORT_DETAIL);
            Long articleVisits = visitRecordRepository.countPageViewsByTypeBetween(startOfDay, endOfDay, VisitRecord.PageType.ARTICLE_DETAIL);
            
            statistics.setPageViews(pageViews);
            statistics.setUniqueVisitors(uniqueVisitors);
            statistics.setHomeVisits(homeVisits);
            statistics.setReportVisits(reportVisits);
            statistics.setArticleVisits(articleVisits);
            
            visitStatisticsRepository.save(statistics);
            
            log.info("完成 {} 的访问数据统计: PV={}, UV={}", yesterday, pageViews, uniqueVisitors);
            
        } catch (Exception e) {
            log.error("每日统计任务执行失败", e);
        }
    }
    
    /**
     * 定时任务：清理旧的访问记录（保留90天）
     */
    @Scheduled(cron = "0 30 2 * * ?") // 每天凌晨02:30执行
    @Transactional
    public void cleanupOldRecords() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(90);
            visitRecordRepository.deleteOldRecords(cutoffTime);
            log.info("清理了90天前的访问记录");
        } catch (Exception e) {
            log.error("清理旧访问记录失败", e);
        }
    }
    
    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 取第一个IP地址
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
} 