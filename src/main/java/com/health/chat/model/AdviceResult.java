package com.health.chat.model;

import java.util.List;

public class AdviceResult {
    private String mainAdvice;
    private List<String> actionableRecommendations;
    private List<ResearchReference> references;

    public AdviceResult() {
    }

    public AdviceResult(String mainAdvice, List<String> actionableRecommendations, 
                       List<ResearchReference> references) {
        this.mainAdvice = mainAdvice;
        this.actionableRecommendations = actionableRecommendations;
        this.references = references;
    }

    public String getMainAdvice() {
        return mainAdvice;
    }

    public void setMainAdvice(String mainAdvice) {
        this.mainAdvice = mainAdvice;
    }

    public List<String> getActionableRecommendations() {
        return actionableRecommendations;
    }

    public void setActionableRecommendations(List<String> actionableRecommendations) {
        this.actionableRecommendations = actionableRecommendations;
    }

    public List<ResearchReference> getReferences() {
        return references;
    }

    public void setReferences(List<ResearchReference> references) {
        this.references = references;
    }
}
