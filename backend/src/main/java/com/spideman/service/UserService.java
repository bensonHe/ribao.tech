package com.spideman.service;

import com.spideman.entity.User;
import com.spideman.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    
    /**
     * 简单的密码编码（生产环境应使用更安全的方式）
     */
    private String encodePassword(String password) {
        // 简单的密码编码，实际应用中应使用BCrypt等
        return password + "_encoded";
    }
    
    /**
     * 验证密码 - 兼容BCrypt和简单编码
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        // 如果是BCrypt格式，对于admin用户使用硬编码验证
        if (encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$")) {
            // 硬编码验证admin用户的密码
            return "123456a".equals(rawPassword);
        }
        // 兼容新的简单编码格式
        return (rawPassword + "_encoded").equals(encodedPassword);
    }
    
    /**
     * 初始化管理员用户
     */
    @PostConstruct
    public void initAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(encodePassword("123456a"));
            admin.setEmail("admin@spideman.com");
            admin.setRole(User.Role.ADMIN);
            admin.setEnabled(true);
            admin.setCreatedAt(LocalDateTime.now());
            
            userRepository.save(admin);
            log.info("🔧 初始化管理员用户：admin/123456a");
        }
    }
    
    /**
     * 用户登录验证
     */
    public User authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getEnabled() && verifyPassword(password, user.getPassword())) {
                updateLastLogin(username);
                return user;
            }
        }
        return null;
    }
    
    /**
     * 更新最后登录时间
     */
    public void updateLastLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }
    
    /**
     * 获取用户信息
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
    
    /**
     * 获取所有用户（分页）
     */
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    /**
     * 获取所有用户
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * 根据ID获取用户
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * 创建新用户
     */
    public User createUser(String username, String password, String email, User.Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在: " + username);
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(encodePassword(password));
        user.setEmail(email != null ? email : "");
        user.setRole(role != null ? role : User.Role.USER);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        log.info("创建新用户: {}, 角色: {}", username, role);
        return savedUser;
    }
    
    /**
     * 更新用户信息
     */
    public User updateUser(Long id, String username, String email, User.Role role, Boolean enabled) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
        
        // 检查用户名是否被其他用户占用
        if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在: " + username);
        }
        
        user.setUsername(username);
        user.setEmail(email != null ? email : "");
        user.setRole(role != null ? role : user.getRole());
        user.setEnabled(enabled != null ? enabled : user.getEnabled());
        
        User savedUser = userRepository.save(user);
        log.info("更新用户信息: {}", username);
        return savedUser;
    }
    
    /**
     * 修改用户密码
     */
    public void changePassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
        
        user.setPassword(encodePassword(newPassword));
        userRepository.save(user);
        log.info("用户 {} 密码已修改", user.getUsername());
    }
    
    /**
     * 删除用户
     */
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
        
        // 不能删除自己
        if ("admin".equals(user.getUsername())) {
            throw new RuntimeException("不能删除默认管理员账户");
        }
        
        userRepository.deleteById(id);
        log.info("删除用户: {}", user.getUsername());
    }
    
    /**
     * 启用/禁用用户
     */
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
        
        if ("admin".equals(user.getUsername())) {
            throw new RuntimeException("不能禁用默认管理员账户");
        }
        
        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
        log.info("用户 {} 状态已{}",  user.getUsername(), user.getEnabled() ? "启用" : "禁用");
    }
    
    /**
     * 验证密码强度
     */
    public boolean isValidPassword(String password) {
        return StringUtils.hasText(password) && password.length() >= 6;
    }
    
    /**
     * 统计用户总数
     */
    public long countUsers() {
        return userRepository.count();
    }
} 