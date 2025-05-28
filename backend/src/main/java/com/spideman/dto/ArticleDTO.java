package com.spideman.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDTO {
    
    private Long id;
    private String title;
    private String titleZh;
    private String summary;
    private String summaryZh;
    private String url;
    private String source;
    private String author;
    private LocalDateTime publishTime;
    private LocalDateTime createdAt;
    private Integer likes;
    private Integer views;
    private String tags;
    private String status;
} 