package com.truthlens.controller;

import com.truthlens.model.AnalyzeRequest;
import com.truthlens.model.AnalyzeResponse;
import com.truthlens.service.NlpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Analyze Controller
 * REST API endpoint for text analysis.
 *
 * Endpoints:
 *   POST /api/analyze  ->  Analyze text for AI vs Human detection
 *   GET  /api/health   ->  Health check
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow Android app to connect
public class AnalyzeController {

    private final NlpService nlpService;

    // Constructor injection
    public AnalyzeController(NlpService nlpService) {
        this.nlpService = nlpService;
    }

    /**
     * Health check endpoint.
     * GET /api/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "TruthLens Spring Boot Backend",
                "version", "1.0.0"
        ));
    }

    /**
     * Analyze text for AI vs Human detection.
     * POST /api/analyze
     *
     * Request Body:
     *   { "text": "Your text to analyze..." }
     *
     * Response:
     *   {
     *     "ai_percentage": 72.5,
     *     "human_percentage": 27.5,
     *     "verdict": "...",
     *     "scores": { ... },
     *     "explanation": [ ... ]
     *   }
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeText(@RequestBody AnalyzeRequest request) {

        // Validate input
        if (request.getText() == null || request.getText().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Text is required. Please provide text to analyze."
            ));
        }

        try {
            // Forward to Python NLP Engine via NlpService
            AnalyzeResponse response = nlpService.analyzeText(request);
            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "error", e.getReason() != null ? e.getReason() : e.getMessage()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Analysis failed: " + e.getMessage()
            ));
        }
    }
}
