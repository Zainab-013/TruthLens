package com.truthlens;

import android.content.Intent;
import android.os.Bundle;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MainActivity - Home Screen
 *
 * User enters/pastes text and clicks "Analyze" button.
 * Text is sent to Spring Boot API via Retrofit.
 * Result is passed to ResultActivity.
 */
public class MainActivity extends AppCompatActivity {

    private EditText editTextInput;
    private TextView textWordCount;
    private Button btnAnalyze;
    private ProgressBar progressBar;
    private TextView textLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        editTextInput = findViewById(R.id.editTextInput);
        textWordCount = findViewById(R.id.textWordCount);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        progressBar = findViewById(R.id.progressBar);
        textLoading = findViewById(R.id.textLoading);

        // Word counter (updates as user types)
        editTextInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString().trim();
                int wordCount = text.isEmpty() ? 0 : text.split("\\s+").length;
                textWordCount.setText(wordCount + " words");

                // Change color based on word count
                if (wordCount < 50) {
                    textWordCount.setTextColor(0xFFE94560); // Red
                } else {
                    textWordCount.setTextColor(0xFF0FFF50); // Green
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Analyze button click
        btnAnalyze.setOnClickListener(v -> {
            String text = editTextInput.getText().toString().trim();

            // Validate
            if (text.isEmpty()) {
                Toast.makeText(this, "Please enter some text!", Toast.LENGTH_SHORT).show();
                return;
            }

            int wordCount = text.split("\\s+").length;
            if (wordCount < 50) {
                Toast.makeText(this, "Please enter at least 50 words (" + wordCount + " currently)", Toast.LENGTH_LONG).show();
                return;
            }

            // Call API
            analyzeText(text);
        });
    }

    /**
     * Send text to Spring Boot API for analysis.
     */
    private void analyzeText(String text) {
        // Show loading
        showLoading(true);

        ApiService apiService = RetrofitClient.getApiService();
        AnalyzeRequest request = new AnalyzeRequest(text);

        apiService.analyzeText(request).enqueue(new Callback<AnalyzeResponse>() {
            @Override
            public void onResponse(Call<AnalyzeResponse> call, Response<AnalyzeResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    AnalyzeResponse result = response.body();

                    // Open ResultActivity with data
                    Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                    intent.putExtra("ai_percentage", result.getAi_percentage());
                    intent.putExtra("human_percentage", result.getHuman_percentage());
                    intent.putExtra("verdict", result.getVerdict());

                    // Convert explanation list to string array
                    String[] explanations = result.getExplanation().toArray(new String[0]);
                    intent.putExtra("explanation", explanations);

                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this,
                            "Analysis failed. Please try again.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AnalyzeResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(MainActivity.this,
                        "Connection failed. Is the server running?\n" + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Show/hide loading indicator.
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        textLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        btnAnalyze.setEnabled(!show);
    }
}
