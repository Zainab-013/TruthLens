package com.truthlens;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.truthlens.api.ApiService;
import com.truthlens.api.RetrofitClient;
import com.truthlens.model.AnalyzeRequest;
import com.truthlens.model.AnalyzeResponse;
import com.truthlens.overlay.FloatingButtonService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MainActivity - Home Screen
 *
 * Two modes:
 * 1. Type/paste text and click "Analyze" → direct analysis
 * 2. Click "Activate Overlay" → floating button appears → capture any screen
 */
public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_CODE = 2001;

    private EditText editTextInput;
    private TextView textWordCount;
    private Button btnAnalyze;
    private Button btnActivateOverlay;
    private Button btnDeactivateOverlay;
    private ProgressBar progressBar;
    private TextView textLoading;

    private boolean isOverlayActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        editTextInput = findViewById(R.id.editTextInput);
        textWordCount = findViewById(R.id.textWordCount);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        btnActivateOverlay = findViewById(R.id.btnActivateOverlay);
        btnDeactivateOverlay = findViewById(R.id.btnDeactivateOverlay);
        progressBar = findViewById(R.id.progressBar);
        textLoading = findViewById(R.id.textLoading);

        // Word counter
        editTextInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString().trim();
                int wordCount = text.isEmpty() ? 0 : text.split("\\s+").length;
                textWordCount.setText(wordCount + " words");
                if (wordCount < 50) {
                    textWordCount.setTextColor(0xFFE94560);
                } else {
                    textWordCount.setTextColor(0xFF0FFF50);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Analyze button
        btnAnalyze.setOnClickListener(v -> {
            String text = editTextInput.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Please enter some text!", Toast.LENGTH_SHORT).show();
                return;
            }
            int wordCount = text.split("\\s+").length;
            if (wordCount < 50) {
                Toast.makeText(this, "Please enter at least 50 words (" + wordCount + " currently)", Toast.LENGTH_LONG).show();
                return;
            }
            analyzeText(text);
        });

        // Activate Overlay button
        btnActivateOverlay.setOnClickListener(v -> {
            activateOverlay();
        });

        // Deactivate Overlay button
        btnDeactivateOverlay.setOnClickListener(v -> {
            deactivateOverlay();
        });
    }

    /**
     * Activate the floating overlay button.
     * Checks for overlay permission first.
     */
    private void activateOverlay() {
        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // Request overlay permission
            Toast.makeText(this, "Please allow overlay permission", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
            return;
        }

        // Start floating button service
        Intent serviceIntent = new Intent(this, FloatingButtonService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        isOverlayActive = true;
        btnActivateOverlay.setVisibility(View.GONE);
        btnDeactivateOverlay.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Overlay activated! Floating button is now on screen.", Toast.LENGTH_SHORT).show();

        // Minimize the app so user can see the floating button
        moveTaskToBack(true);
    }

    /**
     * Deactivate the floating overlay.
     */
    private void deactivateOverlay() {
        Intent serviceIntent = new Intent(this, FloatingButtonService.class);
        stopService(serviceIntent);

        isOverlayActive = false;
        btnActivateOverlay.setVisibility(View.VISIBLE);
        btnDeactivateOverlay.setVisibility(View.GONE);

        Toast.makeText(this, "Overlay deactivated", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                activateOverlay();
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Send text to API for analysis.
     */
    private void analyzeText(String text) {
        showLoading(true);

        ApiService apiService = RetrofitClient.getApiService();
        AnalyzeRequest request = new AnalyzeRequest(text);

        apiService.analyzeText(request).enqueue(new Callback<AnalyzeResponse>() {
            @Override
            public void onResponse(Call<AnalyzeResponse> call, Response<AnalyzeResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AnalyzeResponse result = response.body();
                    Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                    intent.putExtra("ai_percentage", result.getAi_percentage());
                    intent.putExtra("human_percentage", result.getHuman_percentage());
                    intent.putExtra("verdict", result.getVerdict());
                    String[] explanations = result.getExplanation().toArray(new String[0]);
                    intent.putExtra("explanation", explanations);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Analysis failed.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AnalyzeResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(MainActivity.this,
                        "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        textLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        btnAnalyze.setEnabled(!show);
    }
}
