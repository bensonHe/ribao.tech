package com.spideman.config;

import com.spideman.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Autowired
    private UserService userService;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService).passwordEncoder(passwordEncoder());
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                // 公开访问的页面和API
                .antMatchers("/", "/admin").permitAll()
                .antMatchers("/api/articles/**").permitAll()
                .antMatchers("/api/daily-report").permitAll()
                .antMatchers("/api/crawler/health").permitAll()
                // 管理界面需要认证
                .antMatchers("/spideAdmin/**").hasRole("ADMIN")
                // 其他API需要认证
                .antMatchers("/api/crawler/**").hasRole("ADMIN")
                // H2控制台
                .antMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .formLogin()
                .loginPage("/spideAdmin/login")
                .loginProcessingUrl("/spideAdmin/authenticate")
                .successHandler(authenticationSuccessHandler())
                .failureUrl("/spideAdmin/login?error=true")
                .permitAll()
            .and()
            .logout()
                .logoutUrl("/spideAdmin/logout")
                .logoutSuccessUrl("/spideAdmin/login?logout=true")
                .permitAll()
            .and()
            .csrf()
                .ignoringAntMatchers("/h2-console/**", "/api/**")
            .and()
            .headers()
                .frameOptions().sameOrigin(); // 允许H2控制台的iframe
    }
    
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            userService.updateLastLogin(authentication.getName());
            response.sendRedirect("/spideAdmin/dashboard");
        };
    }
    
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
} 