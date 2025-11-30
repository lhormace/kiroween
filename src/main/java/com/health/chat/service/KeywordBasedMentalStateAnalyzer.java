package com.health.chat.service;

import com.health.chat.model.EmotionalTone;
import com.health.chat.model.MentalState;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Keyword-based implementation of MentalStateAnalyzer.
 * Uses emotion keyword dictionaries to analyze mental state from conversation.
 */
public class KeywordBasedMentalStateAnalyzer implements MentalStateAnalyzer {
    
    // Positive emotion keywords (Japanese)
    private static final Set<String> POSITIVE_KEYWORDS = Set.of(
        "嬉しい", "楽しい", "頑張", "やる気", "できた", "達成", "成功",
        "良い", "最高", "素晴らしい", "幸せ", "元気", "調子いい", "順調",
        "やった", "よし", "いいね", "ありがとう", "感謝", "満足"
    );
    
    // Discouraged emotion keywords (Japanese)
    private static final Set<String> DISCOURAGED_KEYWORDS = Set.of(
        "辛い", "疲れ", "しんどい", "無理", "できない", "ダメ", "失敗",
        "悲しい", "落ち込", "憂鬱", "不安", "心配", "困", "苦しい",
        "やめたい", "諦め", "挫折", "ストレス", "イライラ", "つらい"
    );
    
    // Motivation-related keywords
    private static final Set<String> HIGH_MOTIVATION_KEYWORDS = Set.of(
        "頑張", "やる気", "挑戦", "目標", "続け", "継続", "努力",
        "やります", "できる", "やってみ", "トライ", "チャレンジ"
    );
    
    private static final Set<String> LOW_MOTIVATION_KEYWORDS = Set.of(
        "やめたい", "諦め", "無理", "できない", "疲れ", "しんどい",
        "続かない", "やる気ない", "面倒", "だるい"
    );

    @Override
    public MentalState analyze(String message, List<String> conversationHistory) {
        if (message == null || message.trim().isEmpty()) {
            return new MentalState(EmotionalTone.NEUTRAL, 0.5, List.of());
        }

        // Analyze current message
        List<String> indicators = new ArrayList<>();
        int positiveCount = countKeywords(message, POSITIVE_KEYWORDS, indicators, "positive");
        int discouragedCount = countKeywords(message, DISCOURAGED_KEYWORDS, indicators, "discouraged");
        int highMotivationCount = countKeywords(message, HIGH_MOTIVATION_KEYWORDS, indicators, "high_motivation");
        int lowMotivationCount = countKeywords(message, LOW_MOTIVATION_KEYWORDS, indicators, "low_motivation");

        // Consider conversation history for trend analysis
        double historyPositiveRatio = 0.5;
        double historyMotivationRatio = 0.5;
        
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            int historyPositive = 0;
            int historyDiscouraged = 0;
            int historyHighMotivation = 0;
            int historyLowMotivation = 0;
            
            for (String historyMessage : conversationHistory) {
                historyPositive += countKeywords(historyMessage, POSITIVE_KEYWORDS, null, null);
                historyDiscouraged += countKeywords(historyMessage, DISCOURAGED_KEYWORDS, null, null);
                historyHighMotivation += countKeywords(historyMessage, HIGH_MOTIVATION_KEYWORDS, null, null);
                historyLowMotivation += countKeywords(historyMessage, LOW_MOTIVATION_KEYWORDS, null, null);
            }
            
            int totalEmotional = historyPositive + historyDiscouraged;
            if (totalEmotional > 0) {
                historyPositiveRatio = (double) historyPositive / totalEmotional;
            }
            
            int totalMotivation = historyHighMotivation + historyLowMotivation;
            if (totalMotivation > 0) {
                historyMotivationRatio = (double) historyHighMotivation / totalMotivation;
            }
        }

        // Determine emotional tone
        EmotionalTone tone = determineEmotionalTone(
            positiveCount, 
            discouragedCount, 
            historyPositiveRatio
        );

        // Calculate motivation level (0.0 - 1.0)
        double motivationLevel = calculateMotivationLevel(
            highMotivationCount,
            lowMotivationCount,
            positiveCount,
            discouragedCount,
            historyMotivationRatio
        );

        return new MentalState(tone, motivationLevel, indicators);
    }

    /**
     * Counts keywords in message and optionally adds them to indicators list.
     */
    private int countKeywords(String message, Set<String> keywords, 
                             List<String> indicators, String category) {
        if (message == null) {
            return 0;
        }
        
        int count = 0;
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                count++;
                if (indicators != null && category != null) {
                    indicators.add(category + ":" + keyword);
                }
            }
        }
        return count;
    }

    /**
     * Determines emotional tone based on keyword counts and history.
     */
    private EmotionalTone determineEmotionalTone(int positiveCount, 
                                                  int discouragedCount,
                                                  double historyPositiveRatio) {
        // Current message has strong signal
        if (positiveCount > discouragedCount && positiveCount > 0) {
            return EmotionalTone.POSITIVE;
        }
        if (discouragedCount > positiveCount && discouragedCount > 0) {
            return EmotionalTone.DISCOURAGED;
        }
        
        // Use history as tiebreaker or when no clear signal
        if (positiveCount == 0 && discouragedCount == 0) {
            // No emotional keywords in current message, use history
            if (historyPositiveRatio > 0.6) {
                return EmotionalTone.POSITIVE;
            } else if (historyPositiveRatio < 0.4) {
                return EmotionalTone.DISCOURAGED;
            }
        }
        
        return EmotionalTone.NEUTRAL;
    }

    /**
     * Calculates motivation level from 0.0 (low) to 1.0 (high).
     */
    private double calculateMotivationLevel(int highMotivationCount,
                                           int lowMotivationCount,
                                           int positiveCount,
                                           int discouragedCount,
                                           double historyMotivationRatio) {
        // Base motivation from explicit motivation keywords
        double baseMotivation = 0.5;
        
        int totalMotivationKeywords = highMotivationCount + lowMotivationCount;
        if (totalMotivationKeywords > 0) {
            baseMotivation = (double) highMotivationCount / totalMotivationKeywords;
        }
        
        // Adjust based on emotional tone (positive emotions correlate with motivation)
        double emotionalAdjustment = 0.0;
        int totalEmotional = positiveCount + discouragedCount;
        if (totalEmotional > 0) {
            double positiveRatio = (double) positiveCount / totalEmotional;
            emotionalAdjustment = (positiveRatio - 0.5) * 0.3; // Max ±0.15
        }
        
        // Blend with history (70% current, 30% history)
        double currentMotivation = baseMotivation + emotionalAdjustment;
        double blendedMotivation = 0.7 * currentMotivation + 0.3 * historyMotivationRatio;
        
        // Clamp to [0.0, 1.0]
        return Math.max(0.0, Math.min(1.0, blendedMotivation));
    }
}
