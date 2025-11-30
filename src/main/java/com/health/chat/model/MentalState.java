package com.health.chat.model;

import java.util.List;

public class MentalState {
    private EmotionalTone tone;
    private double motivationLevel;
    private List<String> indicators;

    public MentalState() {
    }

    public MentalState(EmotionalTone tone, double motivationLevel, List<String> indicators) {
        this.tone = tone;
        this.motivationLevel = motivationLevel;
        this.indicators = indicators;
    }

    public EmotionalTone getTone() {
        return tone;
    }

    public void setTone(EmotionalTone tone) {
        this.tone = tone;
    }

    public double getMotivationLevel() {
        return motivationLevel;
    }

    public void setMotivationLevel(double motivationLevel) {
        this.motivationLevel = motivationLevel;
    }

    public List<String> getIndicators() {
        return indicators;
    }

    public void setIndicators(List<String> indicators) {
        this.indicators = indicators;
    }
}
