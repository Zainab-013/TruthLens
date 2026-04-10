package com.truthlens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TruthLens Backend - Main Application
 *
 * Spring Boot entry point.
 * Runs on port 8080 (default).
 *
 * Flow:
 *   Android App -> Spring Boot (port 8080) -> Python NLP (port 5000)
 */
@SpringBootApplication
public class TruthLensApplication {

    public static void main(String[] args) {
        SpringApplication.run(TruthLensApplication.class, args);
        System.out.println("==================================================");
        System.out.println("[BACKEND] TruthLens Spring Boot Backend Started!");
        System.out.println("[API]     Running at: http://localhost:8080");
        System.out.println("[ENDPOINT] POST /api/analyze");
        System.out.println("==================================================");
    }
}
