package com.truthlens.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit Client - Singleton
 * Creates and provides a single instance of ApiService.
 *
 * IMPORTANT: Change BASE_URL when deploying to live server.
 * For local testing with emulator, use 10.0.2.2 (emulator's localhost)
 * For real device on same WiFi, use your PC's IP address.
 */
public class RetrofitClient {

    // Default URL to fallback on if none is set
    private static final String DEFAULT_URL = "https://truthlens-backend-0rlr.onrender.com/";
    private static String baseUrl = DEFAULT_URL;

    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    /**
     * Update the base URL dynamically. If it differs from the current baseUrl,
     * the existing Retrofit and ApiService singletons will be reset.
     */
    public static synchronized void updateBaseUrl(String newUrl) {
        if (newUrl != null && !newUrl.isEmpty()) {
            if (!newUrl.endsWith("/")) {
                newUrl += "/";
            }
            if (!newUrl.equals(baseUrl)) {
                baseUrl = newUrl;
                retrofit = null;
                apiService = null;
            }
        }
    }

    /**
     * Get the currently active base URL.
     */
    public static synchronized String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Get the singleton ApiService instance.
     */
    public static synchronized ApiService getApiService() {
        if (apiService == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }
}
