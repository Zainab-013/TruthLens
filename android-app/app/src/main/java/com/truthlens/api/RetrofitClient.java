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

    // ============================================================
    // CHANGE THIS URL BASED ON YOUR SETUP:
    //
    // Emulator:     "http://10.0.2.2:8080/"
    // Real device:  "http://YOUR_PC_IP:8080/"  (e.g., 192.168.1.5)
    // Deployed:     "https://your-api-url.com/"
    // ============================================================
    private static final String BASE_URL = "http://10.0.2.2:8080/";

    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    /**
     * Get the singleton ApiService instance.
     */
    public static ApiService getApiService() {
        if (apiService == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }
}
