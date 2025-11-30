package com.health.chat.model;

public class ChatResponse {
    private String responseText;
    private HealthData extractedData;
    private String advice;
    private TankaPoem tanka;

    public ChatResponse() {
    }

    public ChatResponse(String responseText, HealthData extractedData, String advice, TankaPoem tanka) {
        this.responseText = responseText;
        this.extractedData = extractedData;
        this.advice = advice;
        this.tanka = tanka;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public HealthData getExtractedData() {
        return extractedData;
    }

    public void setExtractedData(HealthData extractedData) {
        this.extractedData = extractedData;
    }

    public String getAdvice() {
        return advice;
    }

    public void setAdvice(String advice) {
        this.advice = advice;
    }

    public TankaPoem getTanka() {
        return tanka;
    }

    public void setTanka(TankaPoem tanka) {
        this.tanka = tanka;
    }
}
