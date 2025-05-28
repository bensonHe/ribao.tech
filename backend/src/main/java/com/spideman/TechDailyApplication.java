package com.spideman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TechDailyApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechDailyApplication.class, args);
        System.out.println("IT技术日报系统启动成功！");
        System.out.println("访问地址: http://localhost:8080");
        
    }
    
} 