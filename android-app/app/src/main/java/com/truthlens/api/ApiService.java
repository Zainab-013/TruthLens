package com.truthlens.api;

import com.truthlens.model.AnalyzeRequest;
import com.truthlens.model.AnalyzeResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Retrofit API interface.
 * Defines the endpoints to call Spring Boot backend.
 */
public interface ApiService {

    /**
     * POST /api/analyze
     * Send text for AI vs Human analysis.
     */
    @POST("api/analyze")
    Call<AnalyzeResponse> analyzeText(@Body AnalyzeRequest request);

    /**
     * GET /api/health
     * Check if backend is running.
     */
    @GET("api/health")
    Call<Object> healthCheck();
}
