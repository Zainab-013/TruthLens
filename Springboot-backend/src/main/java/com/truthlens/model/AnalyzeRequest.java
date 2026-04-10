package com.truthlens.model;

/**
 * Request model for the /api/analyze endpoint.
 * Receives text from Android app.
 */
public class AnalyzeRequest {

    private String text;

    // Default constructor (required by Spring)
    public AnalyzeRequest() {
    }

    public AnalyzeRequest(String text) {
        this.text = text;
    }

    // Getter
    public String getText() {
        return text;
    }

    // Setter
    public void setText(String text) {
        this.text = text;
    }
}
