package com.health.chat.web;

import com.health.chat.model.*;
import com.health.chat.repository.DataRepository;
import com.health.chat.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Controller
public class ChatController {

    @Autowired
    private MessageParser messageParser;
    
    @Autowired
    private NutritionEstimator nutritionEstimator;
    
    @Autowired
    private MentalStateAnalyzer mentalStateAnalyzer;
    
    @Autowired
    private TankaGenerator tankaGenerator;
    
    @Autowired
    private HealthAdvisorAI healthAdvisorAI;
    
    @Autowired(required = false)
    private DataRepository dataRepository;

    @GetMapping("/chat")
    public String chatPage(HttpSession session, Model model) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            return "redirect:/login";
        }
        
        String userId = (String) session.getAttribute("userId");
        model.addAttribute("userId", userId);
        return "chat";
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public ChatResponse sendMessage(@RequestParam String message,
                                   HttpSession session) {
        System.out.println("=== CHAT MESSAGE ===");
        System.out.println("Message: " + message);
        
        String token = (String) session.getAttribute("token");
        String userId = (String) session.getAttribute("userId");
        
        if (token == null || userId == null) {
            System.out.println("Session invalid");
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setResponseText("セッションが無効です。再度ログインしてください。");
            return errorResponse;
        }

        try {
            // 1. メッセージを解析
            HealthData healthData = messageParser.parseMessage(userId, message);
            System.out.println("Parsed health data: weight=" + healthData.getWeight() + 
                             ", bodyFat=" + healthData.getBodyFatPercentage());
            
            // 2. 栄養素を推定
            NutritionInfo nutritionInfo = null;
            if (healthData.getFoodItems() != null && !healthData.getFoodItems().isEmpty()) {
                nutritionInfo = nutritionEstimator.estimateNutrition(healthData.getFoodItems());
                System.out.println("Estimated nutrition: " + nutritionInfo.getCalories() + " kcal");
            }
            
            // 3. 心理状態を分析
            List<String> conversationHistory = new ArrayList<>();
            MentalState mentalState = mentalStateAnalyzer.analyze(message, conversationHistory);
            System.out.println("Mental state: " + mentalState.getTone());
            
            // 4. 健康アドバイスを生成
            UserProfile userProfile = new UserProfile();
            userProfile.setUserId(userId);
            
            AdviceResult adviceResult = healthAdvisorAI.generateAdvice(healthData, mentalState, userProfile);
            System.out.println("Generated advice");
            
            // 5. 短歌を生成
            TankaPoem tanka = tankaGenerator.generate(healthData, mentalState);
            System.out.println("Generated tanka");
            
            // 6. データを保存
            if (dataRepository != null) {
                try {
                    dataRepository.saveHealthData(userId, healthData);
                    if (nutritionInfo != null) {
                        dataRepository.saveNutritionInfo(userId, healthData.getDate(), nutritionInfo);
                    }
                    dataRepository.saveMentalState(userId, healthData.getDate(), mentalState);
                    dataRepository.saveTanka(userId, tanka);
                    System.out.println("Data saved successfully");
                } catch (Exception e) {
                    System.out.println("Warning: Failed to save data: " + e.getMessage());
                }
            }
            
            // 7. レスポンスを構築
            ChatResponse response = new ChatResponse();
            
            StringBuilder responseText = new StringBuilder();
            responseText.append("📊 **健康データ分析結果**\n\n");
            
            // 検出された情報
            if (healthData.getWeight() != null) {
                responseText.append("体重: ").append(healthData.getWeight()).append(" kg\n");
            }
            if (healthData.getBodyFatPercentage() != null) {
                responseText.append("体脂肪率: ").append(healthData.getBodyFatPercentage()).append(" %\n");
            }
            if (healthData.getFoodItems() != null && !healthData.getFoodItems().isEmpty()) {
                responseText.append("食事: ").append(String.join(", ", healthData.getFoodItems())).append("\n");
            }
            if (healthData.getExercises() != null && !healthData.getExercises().isEmpty()) {
                responseText.append("運動: ").append(String.join(", ", healthData.getExercises())).append("\n");
            }
            
            // 栄養情報
            if (nutritionInfo != null) {
                responseText.append("\n🍽️ **栄養情報**\n");
                responseText.append("カロリー: ").append(String.format("%.1f", nutritionInfo.getCalories())).append(" kcal\n");
                responseText.append("タンパク質: ").append(String.format("%.1f", nutritionInfo.getProtein())).append(" g\n");
                responseText.append("脂質: ").append(String.format("%.1f", nutritionInfo.getFat())).append(" g\n");
                responseText.append("炭水化物: ").append(String.format("%.1f", nutritionInfo.getCarbohydrate())).append(" g\n");
            }
            
            // 心理状態
            responseText.append("\n💭 **心理状態**\n");
            responseText.append("トーン: ").append(getMentalStateName(mentalState.getTone())).append("\n");
            responseText.append("モチベーション: ").append(String.format("%.0f", mentalState.getMotivationLevel() * 100)).append("%\n");
            
            // アドバイス
            responseText.append("\n💡 **健康アドバイス**\n");
            responseText.append(adviceResult.getMainAdvice()).append("\n");
            
            if (adviceResult.getActionableRecommendations() != null && 
                !adviceResult.getActionableRecommendations().isEmpty()) {
                responseText.append("\n📝 **推奨事項**\n");
                for (String recommendation : adviceResult.getActionableRecommendations()) {
                    responseText.append("• ").append(recommendation).append("\n");
                }
            }
            
            // 短歌
            responseText.append("\n🌸 **今日の短歌**\n");
            responseText.append(tanka.getLine1()).append("\n");
            responseText.append(tanka.getLine2()).append("\n");
            responseText.append(tanka.getLine3()).append("\n");
            responseText.append(tanka.getLine4()).append("\n");
            responseText.append(tanka.getLine5()).append("\n");
            
            response.setResponseText(responseText.toString());
            response.setExtractedData(healthData);
            response.setTanka(tanka);
            
            System.out.println("Response sent successfully");
            return response;
            
        } catch (Exception e) {
            System.out.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setResponseText("メッセージの処理中にエラーが発生しました: " + e.getMessage());
            return errorResponse;
        }
    }
    
    private String getMentalStateName(EmotionalTone tone) {
        switch (tone) {
            case POSITIVE:
                return "ポジティブ 😊";
            case DISCOURAGED:
                return "落ち込み気味 😔";
            case NEUTRAL:
            default:
                return "ニュートラル 😐";
        }
    }

    @GetMapping("/api/graph")
    @ResponseBody
    public ResponseEntity<String> getGraphData(@RequestParam(defaultValue = "ONE_MONTH") String timeRange,
                                               HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        
        if (userId == null) {
            return ResponseEntity.status(401).body("{\"error\": \"Unauthorized\"}");
        }

        try {
            // 時間範囲を計算
            LocalDate endDate = LocalDate.now();
            LocalDate startDate;
            
            switch (timeRange) {
                case "THREE_MONTHS":
                    startDate = endDate.minusDays(90);
                    break;
                case "SIX_MONTHS":
                    startDate = endDate.minusDays(180);
                    break;
                case "ONE_MONTH":
                default:
                    startDate = endDate.minusDays(30);
                    break;
            }
            
            // データを取得
            List<HealthData> healthDataList = new ArrayList<>();
            if (dataRepository != null) {
                healthDataList = dataRepository.getHealthDataByDateRange(userId, startDate, endDate);
            }
            
            // データをグラフ用に整形
            StringBuilder labels = new StringBuilder();
            StringBuilder weightData = new StringBuilder();
            StringBuilder bodyFatData = new StringBuilder();
            
            // 日付ごとにデータを集約
            Map<LocalDate, HealthData> dataByDate = new TreeMap<>();
            for (HealthData data : healthDataList) {
                if (data.getWeight() != null || data.getBodyFatPercentage() != null) {
                    dataByDate.put(data.getDate(), data);
                }
            }
            
            boolean first = true;
            for (Map.Entry<LocalDate, HealthData> entry : dataByDate.entrySet()) {
                if (!first) {
                    labels.append(", ");
                    weightData.append(", ");
                    bodyFatData.append(", ");
                }
                first = false;
                
                LocalDate date = entry.getKey();
                HealthData data = entry.getValue();
                
                labels.append("\"").append(date.getMonthValue()).append("/").append(date.getDayOfMonth()).append("\"");
                
                if (data.getWeight() != null) {
                    weightData.append(data.getWeight());
                } else {
                    weightData.append("null");
                }
                
                if (data.getBodyFatPercentage() != null) {
                    bodyFatData.append(data.getBodyFatPercentage());
                } else {
                    bodyFatData.append("null");
                }
            }
            
            // データがない場合はダミーデータ
            if (dataByDate.isEmpty()) {
                labels.append("\"データなし\"");
                weightData.append("null");
                bodyFatData.append("null");
            }
            
            String jsonResponse = "{\n" +
                "  \"labels\": [" + labels + "],\n" +
                "  \"datasets\": [\n" +
                "    {\n" +
                "      \"label\": \"体重 (kg)\",\n" +
                "      \"data\": [" + weightData + "],\n" +
                "      \"borderColor\": \"rgb(75, 192, 192)\",\n" +
                "      \"backgroundColor\": \"rgba(75, 192, 192, 0.2)\",\n" +
                "      \"tension\": 0.1,\n" +
                "      \"fill\": true\n" +
                "    },\n" +
                "    {\n" +
                "      \"label\": \"体脂肪率 (%)\",\n" +
                "      \"data\": [" + bodyFatData + "],\n" +
                "      \"borderColor\": \"rgb(255, 99, 132)\",\n" +
                "      \"backgroundColor\": \"rgba(255, 99, 132, 0.2)\",\n" +
                "      \"tension\": 0.1,\n" +
                "      \"fill\": true\n" +
                "    }\n" +
                "  ]\n" +
                "}";
            
            System.out.println("Graph data generated for " + dataByDate.size() + " data points");
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonResponse);
        } catch (Exception e) {
            System.out.println("Error generating graph data: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
