package com.health.chat.service;

import com.health.chat.model.EmotionalTone;
import com.health.chat.model.MentalState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MentalStateAnalyzerTest {
    
    private MentalStateAnalyzer analyzer;
    
    @BeforeEach
    void setUp() {
        analyzer = new KeywordBasedMentalStateAnalyzer();
    }
    
    @Test
    void testPositiveMessage() {
        String message = "今日は体重が減って嬉しい！頑張ります！";
        MentalState state = analyzer.analyze(message, List.of());
        
        assertEquals(EmotionalTone.POSITIVE, state.getTone());
        assertTrue(state.getMotivationLevel() > 0.5);
        assertFalse(state.getIndicators().isEmpty());
    }
    
    @Test
    void testDiscouragedMessage() {
        String message = "体重が増えて辛い。もう無理かも。";
        MentalState state = analyzer.analyze(message, List.of());
        
        assertEquals(EmotionalTone.DISCOURAGED, state.getTone());
        assertTrue(state.getMotivationLevel() < 0.5);
        assertFalse(state.getIndicators().isEmpty());
    }
    
    @Test
    void testNeutralMessage() {
        String message = "今日は体重65kg、体脂肪率20%でした。";
        MentalState state = analyzer.analyze(message, List.of());
        
        assertEquals(EmotionalTone.NEUTRAL, state.getTone());
    }
    
    @Test
    void testEmptyMessage() {
        MentalState state = analyzer.analyze("", List.of());
        
        assertEquals(EmotionalTone.NEUTRAL, state.getTone());
        assertEquals(0.5, state.getMotivationLevel(), 0.01);
    }
    
    @Test
    void testNullMessage() {
        MentalState state = analyzer.analyze(null, List.of());
        
        assertEquals(EmotionalTone.NEUTRAL, state.getTone());
        assertEquals(0.5, state.getMotivationLevel(), 0.01);
    }
    
    @Test
    void testConversationHistoryInfluence() {
        List<String> positiveHistory = List.of(
            "今日も頑張りました！",
            "調子いいです！",
            "目標達成できて嬉しい！"
        );
        
        String neutralMessage = "今日は普通でした。";
        MentalState state = analyzer.analyze(neutralMessage, positiveHistory);
        
        // History should influence toward positive
        assertEquals(EmotionalTone.POSITIVE, state.getTone());
    }
    
    @Test
    void testMotivationCalculation() {
        String highMotivation = "今日も頑張ります！やる気満々です！";
        MentalState state = analyzer.analyze(highMotivation, List.of());
        
        assertTrue(state.getMotivationLevel() > 0.7);
    }
    
    @Test
    void testLowMotivation() {
        String lowMotivation = "もう疲れた。やめたい。";
        MentalState state = analyzer.analyze(lowMotivation, List.of());
        
        assertTrue(state.getMotivationLevel() < 0.3);
    }
}
