package com.truthlens.model;

/**
 * Model class for the API request.
 * Sends text to Spring Boot /api/analyze
 */
public class AnalyzeRequest {

    private String text;

    public AnalyzeRequest(String text) {
        this.text = text;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
