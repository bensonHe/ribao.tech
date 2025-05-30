package com.spideman.controller;

import com.spideman.entity.Article;
import com.spideman.entity.User;
import com.spideman.entity.DailyReport;
import com.spideman.entity.VisitStatistics;
import com.spideman.service.AlibabaAIService;
import com.spideman.service.ArticleService;
import com.spideman.service.CrawlerService;
import com.spideman.service.UserService;
import com.spideman.service.DailyReportService;
import com.spideman.service.VisitStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/spideAdmin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final ArticleService articleService;
    private final CrawlerService crawlerService;
    private final UserService userService;
    private final AlibabaAIService aiService;
    private final DailyReportService dailyReportService;
    private final VisitStatisticsService visitStatisticsService;
    
    /**
     * 登录页面
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "logout", required = false) String logout,
                           Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "用户名或密码错误");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "已成功退出登录");
        }
        return "admin/login";
    }
    
    /**
     * 处理登录认证
     */
    @PostMapping("/authenticate")
    public String authenticate(@RequestParam String username,
                              @RequestParam String password,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = userService.authenticate(username, password);
            if (user != null) {
                // 登录成功，将用户信息存储到session
                session.setAttribute("user", user);
                log.info("用户 {} 登录成功", username);
                return "redirect:/spideAdmin/dashboard";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "用户名或密码错误");
                return "redirect:/spideAdmin/login";
            }
        } catch (Exception e) {
            log.error("登录认证失败", e);
            redirectAttributes.addFlashAttribute("errorMessage", "登录失败: " + e.getMessage());
            return "redirect:/spideAdmin/login";
        }
    }
    
    /**
     * 退出登录
     */
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("successMessage", "已成功退出登录");
        return "redirect:/spideAdmin/login";
    }
    
    /**
     * 管理仪表板
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        try {
            // 临时使用默认用户，因为Spring Security被禁用了
            // User user = userService.getUserByUsername(auth.getName());
            // model.addAttribute("user", user);
            
            // 获取统计信息
            long totalArticles = articleService.countAll();
            long totalUsers = userService.countUsers();
            model.addAttribute("totalArticles", totalArticles);
            model.addAttribute("totalUsers", totalUsers);
            
            // 暂时注释掉访问统计数据，避免数据库表不存在的问题
            /*
            // 获取访问统计数据（添加异常处理）
            try {
                Map<String, Object> visitStats = visitStatisticsService.getOverallStatistics();
                model.addAttribute("visitStats", visitStats);
                
                // 获取最近7天的访问统计
                List<VisitStatistics> last7DaysStats = visitStatisticsService.getLast7DaysStatistics();
                model.addAttribute("last7DaysStats", last7DaysStats);
                
                // 获取今日统计
                VisitStatistics todayStats = visitStatisticsService.getTodayStatistics();
                model.addAttribute("todayStats", todayStats);
            } catch (Exception e) {
                log.error("获取访问统计数据失败", e);
                // 设置默认值
                model.addAttribute("visitStats", new HashMap<>());
                model.addAttribute("last7DaysStats", new ArrayList<>());
                model.addAttribute("todayStats", new VisitStatistics());
            }
            */
            
            // 设置默认的访问统计数据
            Map<String, Object> visitStats = new HashMap<>();
            visitStats.put("totalPageViews", 0L);
            visitStats.put("totalUniqueVisitors", 0L);
            model.addAttribute("visitStats", visitStats);
            model.addAttribute("last7DaysStats", new ArrayList<>());
            model.addAttribute("todayStats", new VisitStatistics());
            
            // 获取可用爬虫源
            model.addAttribute("crawlerSources", crawlerService.getAvailableCrawlers());
            
            // 获取最新文章
            Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
            Page<Article> latestArticles = articleService.getArticles(pageable);
            model.addAttribute("latestArticles", latestArticles.getContent());
            
            return "admin/dashboard";
            
        } catch (Exception e) {
            log.error("加载管理仪表板失败", e);
            model.addAttribute("error", "页面加载失败: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * 文章管理页面
     */
    @GetMapping("/articles")
    public String articlesPage(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(defaultValue = "") String search,
                              @RequestParam(defaultValue = "") String source,
                              Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Article> articles;
        
        if (!search.isEmpty()) {
            articles = articleService.searchArticles(search, pageable);
        } else if (!source.isEmpty()) {
            articles = articleService.getArticlesBySource(source, pageable);
        } else {
            articles = articleService.getArticles(pageable);
        }
        
        model.addAttribute("articles", articles);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("source", source);
        model.addAttribute("sources", crawlerService.getAvailableCrawlers());
        
        return "admin/articles";
    }
    
    /**
     * 爬虫管理页面
     */
    @GetMapping("/crawler")
    public String crawlerPage(Model model) {
        model.addAttribute("sources", crawlerService.getAvailableCrawlers());
        return "admin/crawler";
    }
    
    /**
     * 执行快速爬取
     */
    @PostMapping("/crawler/quick-crawl")
    public String quickCrawl(RedirectAttributes redirectAttributes) {
        try {
            CompletableFuture<Map<String, Object>> future = crawlerService.crawlAllSources(2);
            Map<String, Object> result = future.get();
            
            redirectAttributes.addFlashAttribute("successMessage", 
                String.format("快速爬取完成！成功: %s 篇，失败: %s 篇", 
                    result.get("totalSuccess"), result.get("totalFailed")));
        } catch (Exception e) {
            log.error("快速爬取失败", e);
            redirectAttributes.addFlashAttribute("errorMessage", "爬取失败: " + e.getMessage());
        }
        
        return "redirect:/spideAdmin/crawler";
    }
    
    /**
     * 执行完整爬取
     */
    @PostMapping("/crawler/full-crawl")
    public String fullCrawl(@RequestParam(defaultValue = "5") int articlesPerSource,
                           RedirectAttributes redirectAttributes) {
        try {
            CompletableFuture<Map<String, Object>> future = crawlerService.crawlAllSources(articlesPerSource);
            Map<String, Object> result = future.get();
            
            redirectAttributes.addFlashAttribute("successMessage", 
                String.format("完整爬取完成！成功: %s 篇，失败: %s 篇", 
                    result.get("totalSuccess"), result.get("totalFailed")));
        } catch (Exception e) {
            log.error("完整爬取失败", e);
            redirectAttributes.addFlashAttribute("errorMessage", "爬取失败: " + e.getMessage());
        }
        
        return "redirect:/spideAdmin/crawler";
    }
    
    /**
     * 从指定源爬取
     */
    @PostMapping("/crawler/crawl-source")
    public String crawlFromSource(@RequestParam String source,
                                 @RequestParam(defaultValue = "10") int limit,
                                 RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> result = crawlerService.crawlFromSource(source, limit);
            
            if ((Boolean) result.get("success")) {
                redirectAttributes.addFlashAttribute("successMessage", 
                    String.format("从 %s 爬取完成！总计: %s 篇，成功: %s 篇", 
                        source, result.get("totalCrawled"), result.get("successCount")));
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "爬取失败: " + result.get("message"));
            }
        } catch (Exception e) {
            log.error("指定源爬取失败", e);
            redirectAttributes.addFlashAttribute("errorMessage", "爬取失败: " + e.getMessage());
        }
        
        return "redirect:/spideAdmin/crawler";
    }
    
    /**
     * 删除文章
     */
    @PostMapping("/articles/{id}/delete")
    public String deleteArticle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            articleService.deleteArticle(id);
            redirectAttributes.addFlashAttribute("successMessage", "文章删除成功");
        } catch (Exception e) {
            log.error("删除文章失败", e);
            redirectAttributes.addFlashAttribute("errorMessage", "删除失败: " + e.getMessage());
        }
        
        return "redirect:/spideAdmin/articles";
    }
    
    /**
     * AI翻译文章标题
     */
    @PostMapping("/articles/{id}/translate")
    @ResponseBody
    public Map<String, Object> translateArticle(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            java.util.Optional<?> articleOpt = articleService.getArticleById(id);
            if (!articleOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "文章不存在");
                return result;
            }
            
            Object articleDto = articleOpt.get();
            // 假设ArticleDTO有getTitle方法
            String title = ((com.spideman.dto.ArticleDTO) articleDto).getTitle();
            String translatedTitle = aiService.translateTitle(title);
            
            result.put("success", true);
            result.put("originalTitle", title);
            result.put("translatedTitle", translatedTitle);
            
        } catch (Exception e) {
            log.error("翻译文章标题失败", e);
            result.put("success", false);
            result.put("message", "翻译失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * AI总结文章内容
     */
    @PostMapping("/articles/{id}/summarize")
    @ResponseBody
    public Map<String, Object> summarizeArticle(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            java.util.Optional<?> articleOpt = articleService.getArticleById(id);
            if (!articleOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "文章不存在");
                return result;
            }
            
            Object articleDto = articleOpt.get();
            com.spideman.dto.ArticleDTO article = (com.spideman.dto.ArticleDTO) articleDto;
            String summary = aiService.summarizeArticle(article.getUrl(), article.getTitle());
            result.put("success", true);
            result.put("title", article.getTitle());
            result.put("url", article.getUrl());
            result.put("summary", summary);
            
        } catch (Exception e) {
            log.error("总结文章失败", e);
            result.put("success", false);
            result.put("message", "总结失败: " + e.getMessage());
        }
        
        return result;
    }
    
    // ==================== 用户管理功能 ====================
    
    /**
     * 用户管理页面
     */
    @GetMapping("/users")
    public String usersPage(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = userService.getAllUsers(pageable);
        
        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());
        model.addAttribute("totalUsers", userService.countUsers());
        
        return "admin/users";
    }
    
    /**
     * 新增用户页面
     */
    @GetMapping("/users/new")
    public String newUserPage(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", User.Role.values());
        return "admin/user-form";
    }
    
    /**
     * 编辑用户页面
     */
    @GetMapping("/users/{id}/edit")
    public String editUserPage(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            model.addAttribute("user", user);
            model.addAttribute("roles", User.Role.values());
            model.addAttribute("isEdit", true);
            
            return "admin/user-form";
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            redirectAttributes.addFlashAttribute("errorMessage", "用户不存在: " + e.getMessage());
            return "redirect:/spideAdmin/users";
        }
    }
    
    /**
     * 创建用户
     */
    @PostMapping("/users")
    public String createUser(@RequestParam String username,
                            @RequestParam String password,
                            @RequestParam(required = false) String email,
                            @RequestParam User.Role role,
                            RedirectAttributes redirectAttributes) {
        try {
            if (!userService.isValidPassword(password)) {
                redirectAttributes.addFlashAttribute("errorMessage", "密码长度至少6位");
                return "redirect:/spideAdmin/users/new";
            }
            
            userService.createUser(username, password, email, role);
            redirectAttributes.addFlashAttribute("successMessage", "用户创建成功");
            
        } catch (Exception e) {
            log.error("创建用户失败", e);
            redirectAttributes.addFlashAttribute("errorMessage", "创建失败: " + e.getMessage());
            return "redirect:/spideAdmin/users/new";
        }
        
        return "redirect:/spideAdmin/users";
    }
    
    /**
     * 更新用户
     */
    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id,
                            @RequestParam String username,
                            @RequestParam(required = false) String email,
                            @RequestParam User.Role role,
                            @RequestParam(required = false) Boolean enabled,
                            RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, username, email, role, enabled != null);
            redirectAttributes.addFlashAttribute("successMessage", "用户信息更新成功");
            
        } catch (Exception e) {
            log.error("更新用户失败", e);
            redirectAttributes.addFlashAttribute("errorMessage", "更新失败: " + e.getMessage());
            return "redirect:/spideAdmin/users/" + id + "/edit";
        }
        
        return "redirect:/spideAdmin/users";
    }
    
    /**
     * 修改密码页面
     */
    @GetMapping("/users/{id}/change-password")
    public String changePasswordPage(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            model.addAttribute("user", user);
            return "admin/change-password";
            
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            redirectAttributes.addFlashAttribute("errorMessage", "用户不存在: " + e.getMessage());
            return "redirect:/spideAdmin/users";
        }
    }
    
    /**
     * 修改密码
     */
    @PostMapping("/users/{id}/change-password")
    public String changePassword(@PathVariable Long id,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("errorMessage", "两次输入的密码不一致");
                return "redirect:/spideAdmin/users/" + id + "/change-password";
            }
            
            if (!userService.isValidPassword(newPassword)) {
                redirectAttributes.addFlashAttribute("errorMessage", "密码长度至少6位");
                return "redirect:/spideAdmin/users/" + id + "/change-password";
            }
            
            userService.changePassword(id, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "密码修改成功");
            
        } catch (Exception e) {
            log.error("修改密码失败", e);
            redirectAttributes.addFlashAttribute("errorMessage", "修改失败: " + e.getMessage());
            return "redirect:/spideAdmin/users/" + id + "/change-password";
        }
        
        return "redirect:/spideAdmin/users";
    }
    
    /**
     * 删除用户
     */
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "用户删除成功");
            
        } catch (Exception e) {
            log.error("删除用户失败", e);
            redirectAttributes.addFlashAttribute("errorMessage", "删除失败: " + e.getMessage());
        }
        
        return "redirect:/spideAdmin/users";
    }
    
    /**
     * 启用/禁用用户
     */
    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "用户状态修改成功");
            
        } catch (Exception e) {
            log.error("修改用户状态失败", e);
            redirectAttributes.addFlashAttribute("errorMessage", "操作失败: " + e.getMessage());
        }
        
        return "redirect:/spideAdmin/users";
    }
    
    /**
     * 日报管理页面
     */
    @GetMapping("/reports")
    public String reports(Model model,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size) {
        
        log.info("管理员访问日报管理页面");
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DailyReport> reports = dailyReportService.getReports(pageable);
            
            model.addAttribute("reports", reports);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", reports.getTotalPages());
            model.addAttribute("hasNext", reports.hasNext());
            model.addAttribute("hasPrevious", reports.hasPrevious());
            
            return "admin/reports";
            
        } catch (Exception e) {
            log.error("加载日报管理页面失败", e);
            model.addAttribute("error", "页面加载失败: " + e.getMessage());
            return "admin/dashboard";
        }
    }
    
    /**
     * 新增/编辑日报页面
     */
    @GetMapping("/reports/form")
    public String reportForm(@RequestParam(required = false) Long id,
                            @RequestParam(required = false) String date,
                            Model model) {
        
        try {
            DailyReport report = null;
            
            if (id != null) {
                // 编辑现有日报
                Optional<DailyReport> reportOpt = dailyReportService.getReportById(id);
                
                if (reportOpt.isPresent()) {
                    report = reportOpt.get();
                } else {
                    model.addAttribute("errorMessage", "日报不存在");
                    return "redirect:/spideAdmin/reports";
                }
            } else if (date != null) {
                // 根据日期查找或创建新日报
                LocalDate targetDate = LocalDate.parse(date);
                Optional<DailyReport> reportOpt = dailyReportService.getReportByDate(targetDate);
                
                if (reportOpt.isPresent()) {
                    report = reportOpt.get();
                } else {
                    // 创建新日报
                    report = new DailyReport();
                    report.setReportDate(targetDate);
                    report.setTitle(targetDate + " 技术日报");
                    report.setStatus(DailyReport.ReportStatus.DRAFT);
                }
            } else {
                // 创建今日新日报
                LocalDate today = LocalDate.now();
                report = new DailyReport();
                report.setReportDate(today);
                report.setTitle(today + " 技术日报");
                report.setStatus(DailyReport.ReportStatus.DRAFT);
            }
            
            model.addAttribute("report", report);
            model.addAttribute("isEdit", id != null);
            
            return "admin/report-form";
            
        } catch (Exception e) {
            log.error("加载日报表单失败", e);
            model.addAttribute("errorMessage", "页面加载失败: " + e.getMessage());
            return "redirect:/spideAdmin/reports";
        }
    }
    
    /**
     * 保存日报
     */
    @PostMapping("/reports/save")
    public String saveReport(@RequestParam(required = false) Long id,
                            @RequestParam String reportDate,
                            @RequestParam String title,
                            @RequestParam(required = false) String summary,
                            @RequestParam(required = false) String content,
                            @RequestParam(required = false) String highlights,
                            @RequestParam(required = false) String trends,
                            RedirectAttributes redirectAttributes) {
        
        try {
            if (id != null && id > 0) {
                // 更新现有日报
                dailyReportService.updateReport(id, title, summary, content, highlights, trends);
                redirectAttributes.addFlashAttribute("successMessage", "日报更新成功");
            } else {
                // 创建新日报
                DailyReport report = new DailyReport();
                report.setReportDate(LocalDate.parse(reportDate));
                report.setTitle(title);
                report.setSummary(summary);
                report.setContent(content);
                report.setHighlights(highlights);
                report.setTrends(trends);
                report.setStatus(DailyReport.ReportStatus.PUBLISHED);
                report.setGeneratedAt(LocalDateTime.now());
                
                dailyReportService.saveReport(report);
                redirectAttributes.addFlashAttribute("successMessage", "日报创建成功");
            }
            
        } catch (Exception e) {
            log.error("保存日报失败", e);
            redirectAttributes.addFlashAttribute("errorMessage", "保存失败: " + e.getMessage());
        }
        
        return "redirect:/spideAdmin/reports";
    }
    
    /**
     * 生成指定日期的日报
     */
    @PostMapping("/reports/generate")
    @ResponseBody
    public Map<String, Object> generateReport(@RequestParam String date) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            LocalDate targetDate = LocalDate.parse(date);
            log.info("管理员请求生成 {} 的日报", targetDate);
            
            DailyReport report = dailyReportService.generateDailyReport(targetDate);
            
            result.put("success", true);
            result.put("reportId", report.getId());
            result.put("title", report.getTitle());
            result.put("message", "日报生成成功");
            
        } catch (Exception e) {
            log.error("生成日报失败: {}", date, e);
            result.put("success", false);
            result.put("message", "生成失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 删除日报
     */
    @PostMapping("/reports/{id}/delete")
    public String deleteReport(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            dailyReportService.deleteReport(id);
            redirectAttributes.addFlashAttribute("successMessage", "日报删除成功");
        } catch (Exception e) {
            log.error("删除日报失败: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "删除失败: " + e.getMessage());
        }
        
        return "redirect:/spideAdmin/reports";
    }
    
    /**
     * 手动触发爬虫+日报生成流程（测试用）
     */
    @PostMapping("/test/crawl-and-generate")
    @ResponseBody
    public Map<String, Object> testCrawlAndGenerateReport() {
        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();
        
        try {
            log.info("🧪 手动测试：开始爬虫+日报生成流程");
            
            // 第一步：爬取文章
            crawlerService.crawlAllSources(3) // 测试时只爬取3篇文章
                .thenAccept(crawlResult -> {
                    log.info("✅ 测试爬取完成：成功 {} 篇", crawlResult.get("totalSuccess"));
                    
                    // 第二步：生成当天日报
                    try {
                        dailyReportService.generateDailyReport(today);
                        log.info("✅ 测试日报生成成功");
                    } catch (Exception e) {
                        log.error("❌ 测试日报生成失败", e);
                    }
                })
                .exceptionally(ex -> {
                    log.error("❌ 测试爬取失败", ex);
                    return null;
                });
            
            result.put("success", true);
            result.put("message", "爬虫+日报生成流程已启动，请查看日志获取详细进度");
            result.put("date", today.toString());
            
        } catch (Exception e) {
            log.error("测试流程启动失败", e);
            result.put("success", false);
            result.put("message", "流程启动失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 访问统计页面
     */
    @GetMapping("/statistics")
    public String statisticsPage(Model model,
                                @RequestParam(defaultValue = "7") int days) {
        
        // 获取总体统计数据
        Map<String, Object> overallStats = visitStatisticsService.getOverallStatistics();
        model.addAttribute("overallStats", overallStats);
        
        // 获取指定天数的统计数据
        List<VisitStatistics> recentStats;
        if (days == 30) {
            recentStats = visitStatisticsService.getLast30DaysStatistics();
        } else {
            recentStats = visitStatisticsService.getLast7DaysStatistics();
        }
        model.addAttribute("recentStats", recentStats);
        model.addAttribute("selectedDays", days);
        
        return "admin/statistics";
    }
    
    /**
     * 获取访问统计数据API
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    public Map<String, Object> getStatisticsData(@RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取总体统计
            Map<String, Object> overallStats = visitStatisticsService.getOverallStatistics();
            result.put("overall", overallStats);
            
            // 获取时间序列数据
            List<VisitStatistics> timeSeriesData;
            if (days == 30) {
                timeSeriesData = visitStatisticsService.getLast30DaysStatistics();
            } else {
                timeSeriesData = visitStatisticsService.getLast7DaysStatistics();
            }
            result.put("timeSeries", timeSeriesData);
            
            result.put("success", true);
            
        } catch (Exception e) {
            log.error("获取访问统计数据失败", e);
            result.put("success", false);
            result.put("message", "获取数据失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取访问统计图表数据API
     */
    @GetMapping("/api/statistics/chart")
    @ResponseBody
    public Map<String, Object> getChartData(@RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<VisitStatistics> stats;
            if (days == 30) {
                stats = visitStatisticsService.getLast30DaysStatistics();
            } else {
                stats = visitStatisticsService.getLast7DaysStatistics();
            }
            
            // 构建图表数据
            List<String> dates = new java.util.ArrayList<>();
            List<Long> pageViews = new java.util.ArrayList<>();
            List<Long> uniqueVisitors = new java.util.ArrayList<>();
            List<Long> homeVisits = new java.util.ArrayList<>();
            List<Long> reportVisits = new java.util.ArrayList<>();
            List<Long> articleVisits = new java.util.ArrayList<>();
            
            // 按日期正序排列（图表需要）
            stats.sort((a, b) -> a.getVisitDate().compareTo(b.getVisitDate()));
            
            for (VisitStatistics stat : stats) {
                dates.add(stat.getVisitDate().toString());
                pageViews.add(stat.getPageViews());
                uniqueVisitors.add(stat.getUniqueVisitors());
                homeVisits.add(stat.getHomeVisits());
                reportVisits.add(stat.getReportVisits());
                articleVisits.add(stat.getArticleVisits());
            }
            
            result.put("dates", dates);
            result.put("pageViews", pageViews);
            result.put("uniqueVisitors", uniqueVisitors);
            result.put("homeVisits", homeVisits);
            result.put("reportVisits", reportVisits);
            result.put("articleVisits", articleVisits);
            result.put("success", true);
            
        } catch (Exception e) {
            log.error("获取图表数据失败", e);
            result.put("success", false);
            result.put("message", "获取数据失败: " + e.getMessage());
        }
        
        return result;
    }
} 