package com.truthlens.model;

import java.util.List;
import java.util.Map;

/**
 * Model class for the API response.
 * Maps to the JSON response from Spring Boot /api/analyze
 */
public class AnalyzeResponse {

    private double ai_percentage;
    private double human_percentage;
    private String verdict;
    private Map<String, Object> scores;
    private List<String> explanation;

    // Getters
    public double getAi_percentage() { return ai_percentage; }
    public double getHuman_percentage() { return human_percentage; }
    public String getVerdict() { return verdict; }
    public Map<String, Object> getScores() { return scores; }
    public List<String> getExplanation() { return explanation; }

    // Setters
    public void setAi_percentage(double ai_percentage) { this.ai_percentage = ai_percentage; }
    public void setHuman_percentage(double human_percentage) { this.human_percentage = human_percentage; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public void setScores(Map<String, Object> scores) { this.scores = scores; }
    public void setExplanation(List<String> explanation) { this.explanation = explanation; }
}
