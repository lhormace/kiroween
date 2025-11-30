package com.health.chat.web;

import com.health.chat.model.AuthResult;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

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
        System.out.println("=== LOGIN ATTEMPT ===");
        System.out.println("Username: " + username);
        System.out.println("Password length: " + (password != null ? password.length() : 0));
        
        try {
            // ローカル開発用：簡易認証
            if (username != null && !username.trim().isEmpty() && 
                password != null && !password.trim().isEmpty()) {
                
                String token = "demo-token-" + System.currentTimeMillis();
                session.setAttribute("token", token);
                session.setAttribute("userId", username);
                
                System.out.println("Login successful! Token: " + token);
                return "redirect:/chat";
            } else {
                System.out.println("Login failed: empty credentials");
                model.addAttribute("error", "ユーザー名とパスワードを入力してください");
                return "login";
            }
        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "認証エラー: " + e.getMessage());
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
