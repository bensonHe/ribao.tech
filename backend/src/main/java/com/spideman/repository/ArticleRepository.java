package com.spideman.repository;

import com.spideman.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    
    // 根据URL查找文章（用于去重）
    Optional<Article> findByUrl(String url);
    
    // 根据状态查找文章
    List<Article> findByStatus(Article.ArticleStatus status);
    
    // 分页查询文章
    Page<Article> findByStatusOrderByPublishTimeDesc(Article.ArticleStatus status, Pageable pageable);
    
    // 根据时间范围查询文章
    @Query("SELECT a FROM Article a WHERE a.publishTime BETWEEN :startTime AND :endTime ORDER BY a.publishTime DESC")
    List<Article> findByPublishTimeBetween(@Param("startTime") LocalDateTime startTime, 
                                         @Param("endTime") LocalDateTime endTime);
    
    // 查询今日文章 - 改进版本，使用参数化查询
    @Query("SELECT a FROM Article a WHERE a.publishTime >= :startOfDay AND a.publishTime < :endOfDay ORDER BY a.publishTime DESC")
    List<Article> findTodayArticles(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    // 原始版本（保留作为备用）
    @Query("SELECT a FROM Article a WHERE a.publishTime >= CURRENT_DATE AND a.publishTime < (CURRENT_DATE + 1) ORDER BY a.publishTime DESC")
    List<Article> findTodayArticlesOld();
    
    // 查询最近N天的文章
    @Query("SELECT a FROM Article a WHERE a.publishTime >= :startTime ORDER BY a.publishTime DESC")
    List<Article> findRecentArticles(@Param("startTime") LocalDateTime startTime);
    
    // 统计今日文章数量
    @Query("SELECT COUNT(a) FROM Article a WHERE a.publishTime >= :startOfDay AND a.publishTime < :endOfDay")
    long countTodayArticles(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    // 根据来源查询
    List<Article> findBySourceOrderByPublishTimeDesc(String source);
    
    // 根据来源分页查询
    Page<Article> findBySource(String source, Pageable pageable);
    
    // 搜索文章（标题和内容）
    @Query("SELECT a FROM Article a WHERE a.title LIKE %:keyword% OR a.content LIKE %:keyword% OR a.titleZh LIKE %:keyword% OR a.contentZh LIKE %:keyword%")
    Page<Article> searchArticles(@Param("keyword") String keyword, Pageable pageable);
    
    // 热门文章（按浏览量排序）
    List<Article> findTop10ByStatusOrderByViewsDesc(Article.ArticleStatus status);
} 