package com.health.chat.service;

import com.health.chat.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP-based implementation of HealthAdvisorAI.
 * Generates personalized health advice using research from MCP and adapts tone based on mental state.
 */
public class MCPBasedHealthAdvisor implements HealthAdvisorAI {
    
    private final MCPClient mcpClient;
    
    // Keywords for consultation categorization
    private static final List<String> CONCERN_KEYWORDS = List.of(
        "心配", "不安", "悩み", "困", "辛い", "苦しい", "難しい", "できない",
        "worry", "concern", "anxious", "trouble", "difficult", "hard", "can't"
    );
    
    public MCPBasedHealthAdvisor(MCPClient mcpClient) {
        this.mcpClient = mcpClient;
    }
    
    @Override
    public AdviceResult generateAdvice(HealthData data, MentalState mentalState, UserProfile profile) {
        // Fetch research references from MCP
        List<ResearchReference> references = fetchResearchReferences(data);
        
        // Categorize if this is consultation content
        boolean isConsultation = categorizeAsConsultation(data);
        
        // Generate main advice with appropriate tone
        String mainAdvice = generateMainAdvice(data, mentalState, references, isConsultation);
        
        // Generate actionable recommendations
        List<String> recommendations = generateActionableRecommendations(data, mentalState, references);
        
        return new AdviceResult(mainAdvice, recommendations, references);
    }
    
    /**
     * Fetches research references from MCP based on health data.
     * Requirements 5.1, 5.2, 5.3: Reference nutrition, sleep, and exercise research via MCP
     */
    private List<ResearchReference> fetchResearchReferences(HealthData data) {
        List<ResearchReference> allReferences = new ArrayList<>();
        
        try {
            // Build query based on available health data
            String query = buildResearchQuery(data);
            
            // Fetch all relevant research
            allReferences = mcpClient.fetchAllResearch(query);
            
        } catch (MCPException e) {
            // Log warning and continue without research references
            System.err.println("Warning: MCP unavailable - " + e.getMessage());
            // Return empty list, advice will be generated without research references
        }
        
        return allReferences;
    }
    
    /**
     * Builds a research query based on health data content.
     */
    private String buildResearchQuery(HealthData data) {
        List<String> queryParts = new ArrayList<>();
        
        if (data.getWeight() != null || data.getBodyFatPercentage() != null) {
            queryParts.add("weight management");
        }
        
        if (data.getFoodItems() != null && !data.getFoodItems().isEmpty()) {
            queryParts.add("nutrition");
        }
        
        if (data.getExercises() != null && !data.getExercises().isEmpty()) {
            queryParts.add("exercise physiology");
        }
        
        return queryParts.isEmpty() ? "general health" : String.join(" ", queryParts);
    }
    
    /**
     * Categorizes input as consultation content if it contains concern keywords.
     * Requirement 11.2: Categorize consultation content
     */
    private boolean categorizeAsConsultation(HealthData data) {
        if (data.getFreeComment() == null || data.getFreeComment().isEmpty()) {
            return false;
        }
        
        String comment = data.getFreeComment().toLowerCase();
        return CONCERN_KEYWORDS.stream()
            .anyMatch(keyword -> comment.contains(keyword));
    }
    
    /**
     * Generates main advice with tone adapted to mental state.
     * Requirements 7.4, 7.5: Adapt tone based on mental state
     * Requirements 11.3, 11.4: Provide empathetic responses and incorporate context
     */
    private String generateMainAdvice(HealthData data, MentalState mentalState, 
                                     List<ResearchReference> references, boolean isConsultation) {
        StringBuilder advice = new StringBuilder();
        
        // Add empathetic opening for consultation content (Requirement 11.3)
        if (isConsultation && data.getFreeComment() != null) {
            advice.append(generateEmpatheticOpening(mentalState));
            advice.append(" ");
        }
        
        // Adapt tone based on mental state
        if (mentalState.getTone() == EmotionalTone.POSITIVE) {
            // Requirement 7.4: Encouraging and challenging for positive state
            advice.append(generatePositiveToneAdvice(data, references));
        } else if (mentalState.getTone() == EmotionalTone.DISCOURAGED) {
            // Requirement 7.5: Supportive and gentle for discouraged state
            advice.append(generateSupportiveToneAdvice(data, references));
        } else {
            // Neutral tone
            advice.append(generateNeutralToneAdvice(data, references));
        }
        
        // Incorporate specific context from free comment (Requirement 11.4)
        if (data.getFreeComment() != null && !data.getFreeComment().isEmpty()) {
            advice.append(" ");
            advice.append(incorporateCommentContext(data.getFreeComment()));
        }
        
        return advice.toString();
    }
    
    /**
     * Generates empathetic opening for consultation content.
     * Requirement 11.3: Provide empathetic responses
     */
    private String generateEmpatheticOpening(MentalState mentalState) {
        if (mentalState.getTone() == EmotionalTone.DISCOURAGED) {
            return "お気持ちお察しします。";
        } else {
            return "ご相談ありがとうございます。";
        }
    }
    
    /**
     * Generates advice with positive, encouraging tone.
     * Requirement 7.4: Encouraging and challenging advice for positive state
     */
    private String generatePositiveToneAdvice(HealthData data, List<ResearchReference> references) {
        StringBuilder advice = new StringBuilder();
        advice.append("素晴らしい調子ですね！");
        
        if (data.getWeight() != null) {
            advice.append("体重管理も順調です。");
        }
        
        if (data.getFoodItems() != null && !data.getFoodItems().isEmpty()) {
            advice.append("食事内容も良好です。");
        }
        
        if (data.getExercises() != null && !data.getExercises().isEmpty()) {
            advice.append("運動も継続できていますね。");
        }
        
        // Add research-based insight if available (Requirement 5.4)
        if (!references.isEmpty()) {
            advice.append("最新の研究によると、");
            advice.append(references.get(0).getSummary());
            advice.append("。");
        }
        
        advice.append("この調子で次のステップに挑戦してみましょう！");
        
        return advice.toString();
    }
    
    /**
     * Generates advice with supportive, gentle tone.
     * Requirement 7.5: Supportive and gentle advice for discouraged state
     */
    private String generateSupportiveToneAdvice(HealthData data, List<ResearchReference> references) {
        StringBuilder advice = new StringBuilder();
        advice.append("無理せず、できることから始めましょう。");
        
        if (data.getWeight() != null) {
            advice.append("体重を記録できたこと自体が大きな一歩です。");
        }
        
        if (data.getFoodItems() != null && !data.getFoodItems().isEmpty()) {
            advice.append("食事の記録をつけることで、少しずつ改善していけます。");
        }
        
        // Add research-based encouragement if available (Requirement 5.4)
        if (!references.isEmpty()) {
            advice.append("研究では、");
            advice.append(references.get(0).getSummary());
            advice.append("とされています。");
        }
        
        advice.append("焦らず、ご自身のペースで進めていきましょう。");
        
        return advice.toString();
    }
    
    /**
     * Generates advice with neutral tone.
     */
    private String generateNeutralToneAdvice(HealthData data, List<ResearchReference> references) {
        StringBuilder advice = new StringBuilder();
        advice.append("本日の健康データを確認しました。");
        
        if (data.getWeight() != null) {
            advice.append(String.format("体重は%.1fkgです。", data.getWeight()));
        }
        
        if (data.getBodyFatPercentage() != null) {
            advice.append(String.format("体脂肪率は%.1f%%です。", data.getBodyFatPercentage()));
        }
        
        // Add research-based information if available (Requirement 5.4)
        if (!references.isEmpty()) {
            advice.append("最新の研究では、");
            advice.append(references.get(0).getSummary());
            advice.append("。");
        }
        
        advice.append("継続的な記録が健康管理の鍵となります。");
        
        return advice.toString();
    }
    
    /**
     * Incorporates context from free comment into advice.
     * Requirement 11.4: Incorporate context into advice
     */
    private String incorporateCommentContext(String comment) {
        // Extract key themes from comment and provide relevant guidance
        if (comment.toLowerCase().contains("食事") || comment.toLowerCase().contains("food")) {
            return "食事に関するご相談ですね。バランスの取れた食事を心がけることが大切です。";
        } else if (comment.toLowerCase().contains("運動") || comment.toLowerCase().contains("exercise")) {
            return "運動についてのご質問ですね。無理のない範囲で継続することが重要です。";
        } else if (comment.toLowerCase().contains("睡眠") || comment.toLowerCase().contains("sleep")) {
            return "睡眠に関するお悩みですね。質の良い睡眠は健康の基本です。";
        } else {
            return "ご相談の内容を踏まえて、適切なサポートを提供いたします。";
        }
    }
    
    /**
     * Generates actionable recommendations.
     * Requirement 5.5: Present actionable recommendations
     */
    private List<String> generateActionableRecommendations(HealthData data, MentalState mentalState,
                                                          List<ResearchReference> references) {
        List<String> recommendations = new ArrayList<>();
        
        // Weight-related recommendations
        if (data.getWeight() != null) {
            if (mentalState.getTone() == EmotionalTone.POSITIVE) {
                recommendations.add("体重測定を毎日同じ時間に行い、トレンドを把握しましょう");
            } else {
                recommendations.add("週に2-3回の体重測定から始めてみましょう");
            }
        }
        
        // Nutrition-related recommendations
        if (data.getFoodItems() != null && !data.getFoodItems().isEmpty()) {
            recommendations.add("食事の記録を続けて、栄養バランスを意識しましょう");
            
            // Add research-based recommendation if available
            String nutritionRef = references.stream()
                .filter(ref -> "nutrition".equalsIgnoreCase(ref.getTopic()))
                .map(ResearchReference::getSummary)
                .findFirst()
                .orElse(null);
            
            if (nutritionRef != null) {
                recommendations.add("研究推奨: " + nutritionRef);
            }
        }
        
        // Exercise-related recommendations
        if (data.getExercises() != null && !data.getExercises().isEmpty()) {
            if (mentalState.getTone() == EmotionalTone.POSITIVE) {
                recommendations.add("運動強度を少しずつ上げて、新しいチャレンジを試してみましょう");
            } else {
                recommendations.add("軽い運動から始めて、徐々に習慣化していきましょう");
            }
            
            // Add research-based recommendation if available
            String exerciseRef = references.stream()
                .filter(ref -> "exercise".equalsIgnoreCase(ref.getTopic()))
                .map(ResearchReference::getSummary)
                .findFirst()
                .orElse(null);
            
            if (exerciseRef != null) {
                recommendations.add("研究推奨: " + exerciseRef);
            }
        }
        
        // General recommendation
        if (recommendations.isEmpty()) {
            recommendations.add("毎日の健康記録を継続することが、目標達成への第一歩です");
        }
        
        return recommendations;
    }
}
