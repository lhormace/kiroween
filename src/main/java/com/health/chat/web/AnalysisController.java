package com.health.chat.web;

import com.health.chat.model.EmotionalTone;
import com.health.chat.model.HealthData;
import com.health.chat.model.MentalState;
import com.health.chat.model.NutritionInfo;
import com.health.chat.model.TankaPoem;
import com.health.chat.repository.DataRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Controller
public class AnalysisController {

    @Autowired(required = false)
    private DataRepository dataRepository;

    @GetMapping("/analysis")
    public String analysisPage(HttpSession session, Model model) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            return "redirect:/login";
        }
        
        String userId = (String) session.getAttribute("userId");
        model.addAttribute("userId", userId);
        
        // 最近のデータを取得
        if (dataRepository != null) {
            try {
                LocalDate endDate = LocalDate.now();
                LocalDate startDate = endDate.minusDays(30);
                
                List<HealthData> healthDataList = dataRepository.getHealthDataByDateRange(userId, startDate, endDate);
                List<TankaPoem> tankaList = dataRepository.getTankasByDateRange(userId, startDate, endDate);
                
                model.addAttribute("healthDataCount", healthDataList.size());
                model.addAttribute("tankaCount", tankaList.size());
                model.addAttribute("recentTankas", tankaList.subList(0, Math.min(5, tankaList.size())));
            } catch (Exception e) {
                System.out.println("Warning: Failed to load analysis data: " + e.getMessage());
                model.addAttribute("healthDataCount", 0);
                model.addAttribute("tankaCount", 0);
                model.addAttribute("recentTankas", new ArrayList<>());
            }
        } else {
            model.addAttribute("healthDataCount", 0);
            model.addAttribute("tankaCount", 0);
            model.addAttribute("recentTankas", new ArrayList<>());
        }
        
        return "analysis";
    }

    @GetMapping("/api/analysis/nutrition")
    @ResponseBody
    public ResponseEntity<String> getNutritionAnalysis(@RequestParam(defaultValue = "ONE_MONTH") String timeRange,
                                                       HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        
        if (userId == null) {
            return ResponseEntity.status(401).body("{\"error\": \"Unauthorized\"}");
        }

        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = getStartDate(endDate, timeRange);
            
            if (dataRepository == null) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"averageCalories\": 0, \"averageProtein\": 0, \"averageFat\": 0, \"averageCarbs\": 0}");
            }
            
            // 栄養データを取得
            List<NutritionInfo> nutritionList = dataRepository.getNutritionInfoByDateRange(userId, startDate, endDate);
            
            if (nutritionList.isEmpty()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"averageCalories\": 0, \"averageProtein\": 0, \"averageFat\": 0, \"averageCarbs\": 0}");
            }
            
            // 平均値を計算
            double avgCalories = nutritionList.stream()
                    .mapToDouble(NutritionInfo::getCalories)
                    .average()
                    .orElse(0.0);
            
            double avgProtein = nutritionList.stream()
                    .mapToDouble(NutritionInfo::getProtein)
                    .average()
                    .orElse(0.0);
            
            double avgFat = nutritionList.stream()
                    .mapToDouble(NutritionInfo::getFat)
                    .average()
                    .orElse(0.0);
            
            double avgCarbs = nutritionList.stream()
                    .mapToDouble(NutritionInfo::getCarbohydrate)
                    .average()
                    .orElse(0.0);
            
            String jsonResponse = String.format(
                "{\"averageCalories\": %.1f, \"averageProtein\": %.1f, \"averageFat\": %.1f, \"averageCarbs\": %.1f}",
                avgCalories, avgProtein, avgFat, avgCarbs
            );
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonResponse);
        } catch (Exception e) {
            System.out.println("Error getting nutrition analysis: " + e.getMessage());
            return ResponseEntity.status(500).body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/api/analysis/mental")
    @ResponseBody
    public ResponseEntity<String> getMentalAnalysis(@RequestParam(defaultValue = "ONE_MONTH") String timeRange,
                                                    HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        
        if (userId == null) {
            return ResponseEntity.status(401).body("{\"error\": \"Unauthorized\"}");
        }

        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = getStartDate(endDate, timeRange);
            
            if (dataRepository == null) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"positive\": 0, \"neutral\": 0, \"discouraged\": 0, \"averageMotivation\": 0}");
            }
            
            // メンタルデータを取得
            List<MentalState> mentalStates = dataRepository.getMentalStatesByDateRange(userId, startDate, endDate);
            
            if (mentalStates.isEmpty()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"positive\": 0, \"neutral\": 0, \"discouraged\": 0, \"averageMotivation\": 0}");
            }
            
            // トーンごとにカウント
            long positiveCount = mentalStates.stream()
                    .filter(m -> m.getTone() == EmotionalTone.POSITIVE)
                    .count();
            
            long neutralCount = mentalStates.stream()
                    .filter(m -> m.getTone() == EmotionalTone.NEUTRAL)
                    .count();
            
            long discouragedCount = mentalStates.stream()
                    .filter(m -> m.getTone() == EmotionalTone.DISCOURAGED)
                    .count();
            
            // 平均モチベーション
            double avgMotivation = mentalStates.stream()
                    .mapToDouble(MentalState::getMotivationLevel)
                    .average()
                    .orElse(0.0);
            
            String jsonResponse = String.format(
                "{\"positive\": %d, \"neutral\": %d, \"discouraged\": %d, \"averageMotivation\": %.2f}",
                positiveCount, neutralCount, discouragedCount, avgMotivation
            );
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonResponse);
        } catch (Exception e) {
            System.out.println("Error getting mental analysis: " + e.getMessage());
            return ResponseEntity.status(500).body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/api/analysis/tankas")
    @ResponseBody
    public ResponseEntity<String> getTankas(@RequestParam(defaultValue = "ONE_MONTH") String timeRange,
                                           HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        
        if (userId == null) {
            return ResponseEntity.status(401).body("{\"error\": \"Unauthorized\"}");
        }

        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = getStartDate(endDate, timeRange);
            
            if (dataRepository == null) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"tankas\": []}");
            }
            
            List<TankaPoem> tankaList = dataRepository.getTankasByDateRange(userId, startDate, endDate);
            
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\"tankas\": [");
            
            for (int i = 0; i < tankaList.size(); i++) {
                if (i > 0) jsonBuilder.append(", ");
                
                TankaPoem tanka = tankaList.get(i);
                jsonBuilder.append("{");
                jsonBuilder.append("\"date\": \"").append(tanka.getDate()).append("\", ");
                jsonBuilder.append("\"lines\": [");
                jsonBuilder.append("\"").append(escapeJson(tanka.getLine1())).append("\", ");
                jsonBuilder.append("\"").append(escapeJson(tanka.getLine2())).append("\", ");
                jsonBuilder.append("\"").append(escapeJson(tanka.getLine3())).append("\", ");
                jsonBuilder.append("\"").append(escapeJson(tanka.getLine4())).append("\", ");
                jsonBuilder.append("\"").append(escapeJson(tanka.getLine5())).append("\"");
                jsonBuilder.append("]");
                jsonBuilder.append("}");
            }
            
            jsonBuilder.append("]}");
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBuilder.toString());
        } catch (Exception e) {
            System.out.println("Error getting tankas: " + e.getMessage());
            return ResponseEntity.status(500).body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private LocalDate getStartDate(LocalDate endDate, String timeRange) {
        switch (timeRange) {
            case "THREE_MONTHS":
                return endDate.minusDays(90);
            case "SIX_MONTHS":
                return endDate.minusDays(180);
            case "ONE_MONTH":
            default:
                return endDate.minusDays(30);
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
