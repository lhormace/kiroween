package com.health.chat.model;

public class ResearchReference {
    private String topic;
    private String summary;
    private String source;

    public ResearchReference() {
    }

    public ResearchReference(String topic, String summary, String source) {
        this.topic = topic;
        this.summary = summary;
        this.source = source;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
