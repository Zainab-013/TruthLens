package com.truthlens.model;

import java.util.List;

/**
 * Model class for a cached history item.
 */
public class HistoryItem {
    private String text;
    private double aiPercentage;
    private double humanPercentage;
    private String verdict;
    private List<String> explanations;
    private long timestamp;

    public HistoryItem(String text, double aiPercentage, double humanPercentage, String verdict, List<String> explanations) {
        this.text = text;
        this.aiPercentage = aiPercentage;
        this.humanPercentage = humanPercentage;
        this.verdict = verdict;
        this.explanations = explanations;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getText() { return text; }
    public double getAiPercentage() { return aiPercentage; }
    public double getHumanPercentage() { return humanPercentage; }
    public String getVerdict() { return verdict; }
    public List<String> getExplanations() { return explanations; }
    public long getTimestamp() { return timestamp; }

    // Setters
    public void setText(String text) { this.text = text; }
    public void setAiPercentage(double aiPercentage) { this.aiPercentage = aiPercentage; }
    public void setHumanPercentage(double humanPercentage) { this.humanPercentage = humanPercentage; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public void setExplanations(List<String> explanations) { this.explanations = explanations; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
