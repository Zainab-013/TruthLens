package com.truthlens.model;

import java.util.List;
import java.util.Map;

/**
 * Response model for the /api/analyze endpoint.
 * Returns AI vs Human detection results to Android app.
 *
 * Example Response:
 * {
 *   "ai_percentage": 72.5,
 *   "human_percentage": 27.5,
 *   "verdict": "This text is very likely AI-generated.",
 *   "scores": { "perplexity": 0.05, ... },
 *   "explanation": ["Low perplexity detected...", ...]
 * }
 */
public class AnalyzeResponse {

    private double ai_percentage;
    private double human_percentage;
    private String verdict;
    private Map<String, Object> scores;
    private List<String> explanation;

    // Default constructor
    public AnalyzeResponse() {
    }

    // Getters and Setters
    public double getAi_percentage() {
        return ai_percentage;
    }

    public void setAi_percentage(double ai_percentage) {
        this.ai_percentage = ai_percentage;
    }

    public double getHuman_percentage() {
        return human_percentage;
    }

    public void setHuman_percentage(double human_percentage) {
        this.human_percentage = human_percentage;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public Map<String, Object> getScores() {
        return scores;
    }

    public void setScores(Map<String, Object> scores) {
        this.scores = scores;
    }

    public List<String> getExplanation() {
        return explanation;
    }

    public void setExplanation(List<String> explanation) {
        this.explanation = explanation;
    }
}
