package com.spideman.controller;

import com.spideman.entity.Article;
import com.spideman.entity.User;
import com.spideman.service.AlibabaAIService;
import com.spideman.service.ArticleService;
import com.spideman.service.CrawlerService;
import com.spideman.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Controller
@RequestMapping("/spideAdmin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final ArticleService articleService;
    private final CrawlerService crawlerService;
    private final UserService userService;
    private final AlibabaAIService aiService;
    
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
     * 管理仪表板
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model, Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        model.addAttribute("user", user);
        
        // 获取统计信息
        long totalArticles = articleService.countAll();
        long totalUsers = userService.countUsers();
        model.addAttribute("totalArticles", totalArticles);
        model.addAttribute("totalUsers", totalUsers);
        
        // 获取可用爬虫源
        model.addAttribute("crawlerSources", crawlerService.getAvailableCrawlers());
        
        // 获取最新文章
        Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        Page<Article> latestArticles = articleService.getArticles(pageable);
        model.addAttribute("latestArticles", latestArticles.getContent());
        
        return "admin/dashboard";
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
} 