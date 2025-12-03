package com.health.chat.web;

import com.health.chat.model.AuthResult;
import com.health.chat.service.AuthenticationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

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
                    session.setAttribute("token", result.getToken());
                    session.setAttribute("userId", result.getUserId());
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
        System.out.println("=== REGISTER PAGE REQUEST ===");
        if (session.getAttribute("token") != null) {
            return "redirect:/chat";
        }
        return "register";
    }
    
    @GetMapping("/test-register")
    @org.springframework.web.bind.annotation.ResponseBody
    public String testRegister() {
        return "AuthController is working! AuthService: " + (authenticationService != null);
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String email,
                          HttpSession session,
                          Model model) {
        System.out.println("=== REGISTER REQUEST RECEIVED ===");
        System.out.println("Username: " + username);
        System.out.println("Email: " + email);
        System.out.println("AuthService available: " + (authenticationService != null));
        
        try {
            // Use authentication service if available
            if (authenticationService != null) {
                System.out.println("Calling registerUser...");
                AuthResult result = authenticationService.registerUser(username, password, email);
                System.out.println("Registration result - Success: " + result.isSuccess());
                System.out.println("Registration result - Error: " + result.getErrorMessage());
                
                if (result.isSuccess()) {
                    session.setAttribute("token", result.getToken());
                    session.setAttribute("userId", result.getUserId());
                    System.out.println("Registration successful, redirecting to /chat");
                    return "redirect:/chat";
                } else {
                    System.out.println("Registration failed: " + result.getErrorMessage());
                    model.addAttribute("error", result.getErrorMessage());
                    return "register";
                }
            } else {
                System.out.println("AuthenticationService is null!");
                model.addAttribute("error", "ユーザー登録機能は現在利用できません");
                return "register";
            }
        } catch (Exception e) {
            System.out.println("Exception during registration: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "登録エラーが発生しました");
            return "register";
        }
    }
}
