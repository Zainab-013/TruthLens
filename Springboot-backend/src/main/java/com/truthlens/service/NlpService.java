package com.truthlens.service;

import com.truthlens.model.AnalyzeRequest;
import com.truthlens.model.AnalyzeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

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

    private final RestClient restClient;

    /**
     * Constructor - creates RestClient with Python API base URL.
     * URL is configured in application.properties
     */
    public NlpService(@Value("${nlp.api.url}") String nlpApiUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(30000);

        this.restClient = RestClient.builder()
                .baseUrl(nlpApiUrl)
                .requestFactory(requestFactory)
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
            return restClient.post()
                    .uri("/analyze")
                    .body(request)
                    .retrieve()
                    .body(AnalyzeResponse.class);
        } catch (RestClientResponseException e) {
            String errorDetail = e.getResponseBodyAsString();
            try {
                Map<?, ?> map = e.getResponseBodyAs(Map.class);
                if (map != null && map.containsKey("detail")) {
                    errorDetail = map.get("detail").toString();
                }
            } catch (Exception ignored) {
            }
            throw new ResponseStatusException(e.getStatusCode(), errorDetail, e);
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to connect to NLP Engine: " + e.getMessage(), e
            );
        }
    }
}
