package com.truthlens.service;

import com.truthlens.model.AnalyzeRequest;
import com.truthlens.model.AnalyzeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * NLP Service
 * Connects Spring Boot to the Python NLP Engine.
 *
 * Flow:
 *   AnalyzeController -> NlpService -> Python API (POST /analyze)
 *                                          |
 *                                    Returns result
 */
@Service
public class NlpService {

    private final WebClient webClient;

    /**
     * Constructor - creates WebClient with Python API base URL.
     * URL is configured in application.properties
     */
    public NlpService(@Value("${nlp.api.url}") String nlpApiUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(nlpApiUrl)
                .build();
    }

    /**
     * Send text to Python NLP API for analysis.
     *
     * @param request The text to analyze
     * @return AnalyzeResponse with AI%, Human%, scores, explanation
     */
    public AnalyzeResponse analyzeText(AnalyzeRequest request) {
        try {
            AnalyzeResponse response = webClient.post()
                    .uri("/analyze")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AnalyzeResponse.class)
                    .block(); // Synchronous call

            return response;

        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to connect to NLP Engine: " + e.getMessage(), e
            );
        }
    }
}
