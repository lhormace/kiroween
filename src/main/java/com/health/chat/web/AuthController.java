package com.health.chat.web;

import com.health.chat.model.AuthResult;
import com.health.chat.service.AuthenticationService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;

@Controller
public class AuthController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    @Autowired(required = false)
    private AuthenticationService authenticationService;

    @GetMapping("/")
    public String index(HttpSession session) {
        if (session.getAttribute("token") != null) {
            return "redirect:/chat";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("token") != null) {
            return "redirect:/chat";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, 
                       @RequestParam String password,
                       HttpSession session,
                       Model model) {
        try {
            // Use authentication service
            if (authenticationService != null) {
                AuthResult result = authenticationService.authenticate(username, password);
                
                if (result.isSuccess()) {
                    // Set session attributes
                    session.setAttribute("token", result.getToken());
                    session.setAttribute("userId", result.getUserId());
                    
                    // Set Spring Security authentication
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            result.getUserId(), 
                            null, 
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, 
                                       SecurityContextHolder.getContext());
                    
                    return "redirect:/chat";
                } else {
                    model.addAttribute("error", result.getErrorMessage());
                    return "login";
                }
            } else {
                // Fallback for local development only
                String localMode = System.getenv("LOCAL_DEV_MODE");
                if ("true".equals(localMode)) {
                    if (username != null && !username.trim().isEmpty() && 
                        password != null && !password.trim().isEmpty()) {
                        String token = "demo-token-" + System.currentTimeMillis();
                        session.setAttribute("token", token);
                        session.setAttribute("userId", username);
                        return "redirect:/chat";
                    }
                }
                model.addAttribute("error", "認証サービスが利用できません");
                return "login";
            }
        } catch (Exception e) {
            model.addAttribute("error", "認証エラーが発生しました");
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (session.getAttribute("token") != null) {
            return "redirect:/chat";
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String email,
                          HttpSession session,
                          Model model) {
        LOGGER.info("=== REGISTER REQUEST ===");
        LOGGER.info("Username: {}", username);
        LOGGER.info("Email: {}", email);
        LOGGER.info("AuthService available: {}", (authenticationService != null));
        
        try {
            // Use authentication service if available
            if (authenticationService != null) {
                LOGGER.info("Calling registerUser...");
                AuthResult result = authenticationService.registerUser(username, password, email);
                LOGGER.info("Result success: {}", result.isSuccess());
                LOGGER.info("Result error: {}", result.getErrorMessage());
                
                if (result.isSuccess()) {
                    // Set session attributes
                    session.setAttribute("token", result.getToken());
                    session.setAttribute("userId", result.getUserId());
                    
                    // Set Spring Security authentication
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            result.getUserId(), 
                            null, 
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, 
                                       SecurityContextHolder.getContext());
                    
                    LOGGER.info("Registration successful, redirecting to /chat");
                    return "redirect:/chat";
                } else {
                    LOGGER.warn("Registration failed: {}", result.getErrorMessage());
                    model.addAttribute("error", result.getErrorMessage());
                    return "register";
                }
            } else {
                LOGGER.error("AuthenticationService is null!");
                model.addAttribute("error", "ユーザー登録機能は現在利用できません");
                return "register";
            }
        } catch (Exception e) {
            LOGGER.error("Exception during registration", e);
            model.addAttribute("error", "登録エラーが発生しました: " + e.getMessage());
            return "register";
        }
    }
}
