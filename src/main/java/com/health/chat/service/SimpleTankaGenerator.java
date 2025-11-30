package com.health.chat.service;

import com.health.chat.model.EmotionalTone;
import com.health.chat.model.HealthData;
import com.health.chat.model.MentalState;
import com.health.chat.model.TankaPoem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimpleTankaGenerator implements TankaGenerator {
    
    private final Random random = new Random();
    
    @Override
    public TankaPoem generate(HealthData data, MentalState mentalState) {
        LocalDate date = data.getDate() != null ? data.getDate() : LocalDate.now();
        
        // Extract key health events
        List<String> events = extractKeyEvents(data, mentalState);
        
        // Generate tanka lines with 5-7-5-7-7 structure
        String line1 = generateLine(5, events, 0);
        String line2 = generateLine(7, events, 1);
        String line3 = generateLine(5, events, 2);
        String line4 = generateLine(7, events, 3);
        String line5 = generateLine(7, events, 4);
        
        return new TankaPoem(line1, line2, line3, line4, line5, date);
    }
    
    /**
     * Count Japanese syllables (mora) in a string
     * Japanese mora counting rules:
     * - Each hiragana/katakana character = 1 mora
     * - Small tsu (っ/ッ) = 1 mora
     * - Long vowel mark (ー) = 1 mora
     * - Kanji characters are counted by their reading
     */
    public int countMora(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (char c : text.toCharArray()) {
            // Hiragana range: 0x3040-0x309F
            // Katakana range: 0x30A0-0x30FF
            // Small tsu: っ (0x3063) or ッ (0x30C3)
            // Long vowel: ー (0x30FC)
            if ((c >= 0x3040 && c <= 0x309F) || (c >= 0x30A0 && c <= 0x30FF)) {
                count++;
            }
            // For kanji (0x4E00-0x9FFF), estimate 2 mora per character
            else if (c >= 0x4E00 && c <= 0x9FFF) {
                count += 2;
            }
        }
        
        return count;
    }
    
    private List<String> extractKeyEvents(HealthData data, MentalState mentalState) {
        List<String> events = new ArrayList<>();
        
        // Add weight information
        if (data.getWeight() != null) {
            events.add("体重" + formatNumber(data.getWeight()) + "キロ");
        }
        
        // Add body fat information
        if (data.getBodyFatPercentage() != null) {
            events.add("体脂肪" + formatNumber(data.getBodyFatPercentage()) + "パーセント");
        }
        
        // Add food items
        if (data.getFoodItems() != null && !data.getFoodItems().isEmpty()) {
            for (String food : data.getFoodItems()) {
                events.add(food + "を食べた");
            }
        }
        
        // Add exercise items
        if (data.getExercises() != null && !data.getExercises().isEmpty()) {
            for (String exercise : data.getExercises()) {
                events.add(exercise + "をした");
            }
        }
        
        // Add emotional state
        if (mentalState != null && mentalState.getTone() != null) {
            switch (mentalState.getTone()) {
                case POSITIVE:
                    events.add("元気に過ごす");
                    events.add("前向きな日");
                    break;
                case DISCOURAGED:
                    events.add("少し疲れた");
                    events.add("休息が必要");
                    break;
                case NEUTRAL:
                    events.add("穏やかな日");
                    events.add("日々の記録");
                    break;
            }
        }
        
        // Add free comment if available
        if (data.getFreeComment() != null && !data.getFreeComment().isEmpty()) {
            events.add(data.getFreeComment());
        }
        
        return events;
    }
    
    private String formatNumber(double number) {
        if (number == (long) number) {
            return String.valueOf((long) number);
        } else {
            return String.format("%.1f", number);
        }
    }
    
    private String generateLine(int targetMora, List<String> events, int lineIndex) {
        // Template-based generation with target mora count
        List<String> templates = getTemplatesForMora(targetMora, lineIndex);
        
        if (templates.isEmpty()) {
            // Fallback: generate simple line
            return generateSimpleLine(targetMora, events);
        }
        
        // Select a template
        String template = templates.get(random.nextInt(templates.size()));
        
        // Try to incorporate events into the template
        return fillTemplate(template, events, targetMora);
    }
    
    private List<String> getTemplatesForMora(int mora, int lineIndex) {
        List<String> templates = new ArrayList<>();
        
        if (mora == 5) {
            templates.add("今日の記録");
            templates.add("健康の道");
            templates.add("日々の努力");
            templates.add("体を測る");
            templates.add("食事の記録");
            templates.add("運動の日");
            templates.add("心と体");
        } else if (mora == 7) {
            templates.add("健康を目指して");
            templates.add("日々の積み重ね");
            templates.add("体重を記録し");
            templates.add("食事を振り返る");
            templates.add("運動を続ける");
            templates.add("前向きに進む");
            templates.add("穏やかな気持ち");
        }
        
        return templates;
    }
    
    private String generateSimpleLine(int targetMora, List<String> events) {
        // Try to use events to create a line
        for (String event : events) {
            int mora = countMora(event);
            if (mora == targetMora) {
                return event;
            } else if (mora < targetMora) {
                // Try to pad with particles
                String padded = padToMora(event, targetMora);
                if (countMora(padded) == targetMora) {
                    return padded;
                }
            }
        }
        
        // Fallback to generic phrases
        return getGenericPhrase(targetMora);
    }
    
    private String fillTemplate(String template, List<String> events, int targetMora) {
        // For now, just return the template
        // In a more sophisticated implementation, we would replace placeholders
        int mora = countMora(template);
        if (mora == targetMora) {
            return template;
        }
        
        // Try to adjust
        return adjustToMora(template, targetMora);
    }
    
    private String padToMora(String text, int targetMora) {
        int currentMora = countMora(text);
        if (currentMora >= targetMora) {
            return text;
        }
        
        int needed = targetMora - currentMora;
        
        // Add particles or words to reach target
        if (needed == 1) {
            return text + "よ";
        } else if (needed == 2) {
            return text + "かな";
        } else if (needed == 3) {
            return text + "だろう";
        }
        
        return text;
    }
    
    private String adjustToMora(String text, int targetMora) {
        int currentMora = countMora(text);
        
        if (currentMora == targetMora) {
            return text;
        } else if (currentMora < targetMora) {
            return padToMora(text, targetMora);
        } else {
            // Truncate or simplify
            // For now, just return as is
            return text;
        }
    }
    
    private String getGenericPhrase(int mora) {
        if (mora == 5) {
            return "今日の記録";
        } else if (mora == 7) {
            return "健康を目指して";
        }
        return "記録";
    }
}
