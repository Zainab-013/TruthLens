package com.truthlens;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.text.InputType;
import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.truthlens.api.ApiService;
import com.truthlens.api.RetrofitClient;
import com.truthlens.model.AnalyzeRequest;
import com.truthlens.model.AnalyzeResponse;
import com.truthlens.model.HistoryItem;
import com.truthlens.overlay.FloatingButtonService;
import com.truthlens.utils.HistoryManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MainActivity - Home Screen
 *
 * Features:
 * 1. Type/paste text manually → Analyze
 * 2. Paste from clipboard button
 * 3. Activate floating overlay (if supported)
 */
public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_CODE = 2001;

    private EditText editTextInput;
    private TextView textWordCount;
    private Button btnAnalyze;
    private Button btnPasteClipboard;
    private Button btnActivateOverlay;
    private Button btnDeactivateOverlay;
    private Button btnViewHistory;
    private ProgressBar progressBar;
    private TextView textLoading;
    private TextView textCurrentServer;
    private Button btnConfigureServer;

    private boolean isOverlayActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load stored server URL from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("TruthLensPrefs", MODE_PRIVATE);
        String savedUrl = prefs.getString("backend_url", "https://truthlens-backend-0rlr.onrender.com/");
        RetrofitClient.updateBaseUrl(savedUrl);

        // Initialize views safely
        editTextInput = findViewById(R.id.editTextInput);
        textWordCount = findViewById(R.id.textWordCount);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        btnPasteClipboard = findViewById(R.id.btnPasteClipboard);
        btnActivateOverlay = findViewById(R.id.btnActivateOverlay);
        btnDeactivateOverlay = findViewById(R.id.btnDeactivateOverlay);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        progressBar = findViewById(R.id.progressBar);
        textLoading = findViewById(R.id.textLoading);
        textCurrentServer = findViewById(R.id.textCurrentServer);
        btnConfigureServer = findViewById(R.id.btnConfigureServer);

        if (textCurrentServer != null) {
            textCurrentServer.setText("Current: " + RetrofitClient.getBaseUrl());
        }

        if (btnConfigureServer != null) {
            btnConfigureServer.setOnClickListener(v -> showServerConfigDialog());
        }

        // Word counter
        if (editTextInput != null) {
            editTextInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String text = s.toString().trim();
                    int wordCount = text.isEmpty() ? 0 : text.split("\\s+").length;
                    if (textWordCount != null) {
                        textWordCount.setText(wordCount + " words");
                        textWordCount.setTextColor(wordCount < 50 ? 0xFFE94560 : 0xFF0FFF50);
                    }
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Paste from clipboard
        if (btnPasteClipboard != null) {
            btnPasteClipboard.setOnClickListener(v -> pasteFromClipboard());
        }

        // Analyze button
        if (btnAnalyze != null) {
            btnAnalyze.setOnClickListener(v -> {
                String text = editTextInput.getText().toString().trim();
                if (text.isEmpty()) {
                    Toast.makeText(this, "Please enter some text!", Toast.LENGTH_SHORT).show();
                    return;
                }
                int wordCount = text.split("\\s+").length;
                if (wordCount < 50) {
                    Toast.makeText(this, "Need at least 50 words (" + wordCount + " currently)", Toast.LENGTH_LONG).show();
                    return;
                }
                analyzeText(text);
            });
        }

        // Activate Overlay
        if (btnActivateOverlay != null) {
            btnActivateOverlay.setOnClickListener(v -> activateOverlay());
        }

        // Deactivate Overlay
        if (btnDeactivateOverlay != null) {
            btnDeactivateOverlay.setOnClickListener(v -> deactivateOverlay());
        }

        // View History
        if (btnViewHistory != null) {
            btnViewHistory.setOnClickListener(v -> {
                Intent intent = new Intent(this, HistoryActivity.class);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateOverlayButtons();
    }

    private void updateOverlayButtons() {
        if (FloatingButtonService.isRunning) {
            if (btnActivateOverlay != null) btnActivateOverlay.setVisibility(View.GONE);
            if (btnDeactivateOverlay != null) btnDeactivateOverlay.setVisibility(View.VISIBLE);
        } else {
            if (btnActivateOverlay != null) btnActivateOverlay.setVisibility(View.VISIBLE);
            if (btnDeactivateOverlay != null) btnDeactivateOverlay.setVisibility(View.GONE);
        }
    }

    /**
     * Paste text from clipboard into input field
     */
    private void pasteFromClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence pastedText = clip.getItemAt(0).getText();
                    if (pastedText != null && pastedText.length() > 0) {
                        editTextInput.setText(pastedText);
                        Toast.makeText(this, "Text pasted from clipboard!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            Toast.makeText(this, "No text in clipboard", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not paste: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Activate floating overlay - with full error handling
     */
    private void activateOverlay() {
        try {
            // Check overlay permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please enable 'Display over other apps' permission", Toast.LENGTH_LONG).show();
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

            Toast.makeText(this, "Overlay activated! Look for the red TL button.", Toast.LENGTH_LONG).show();
            moveTaskToBack(true);

        } catch (Exception e) {
            Toast.makeText(this, "Overlay not supported on this device. Use manual text input instead.",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Deactivate floating overlay
     */
    private void deactivateOverlay() {
        try {
            Intent serviceIntent = new Intent(this, FloatingButtonService.class);
            stopService(serviceIntent);
        } catch (Exception ignored) {}

        isOverlayActive = false;
        if (btnActivateOverlay != null) btnActivateOverlay.setVisibility(View.VISIBLE);
        if (btnDeactivateOverlay != null) btnDeactivateOverlay.setVisibility(View.GONE);
        Toast.makeText(this, "Overlay deactivated", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                activateOverlay();
            } else {
                Toast.makeText(this, "Permission denied. Use manual text input.", Toast.LENGTH_LONG).show();
            }
        }
    }

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
                    
                    // Save to local cache history
                    HistoryManager.saveHistoryItem(MainActivity.this, 
                        new HistoryItem(
                            text, 
                            result.getAi_percentage(), 
                            result.getHuman_percentage(), 
                            result.getVerdict(), 
                            result.getExplanation()
                        )
                    );
                    
                    Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                    intent.putExtra("ai_percentage", result.getAi_percentage());
                    intent.putExtra("human_percentage", result.getHuman_percentage());
                    intent.putExtra("verdict", result.getVerdict());
                    intent.putExtra("explanation", result.getExplanation().toArray(new String[0]));
                    startActivity(intent);

                } else {
                    Toast.makeText(MainActivity.this, "Analysis failed. Try again.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AnalyzeResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(MainActivity.this, "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (textLoading != null) textLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        if (btnAnalyze != null) btnAnalyze.setEnabled(!show);
    }

    private void showServerConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configure Server Connection");

        // Container to hold input field and helper text with custom padding
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        final EditText input = new EditText(this);
        input.setText(RetrofitClient.getBaseUrl());
        input.setHint("http://192.168.x.x:8080/");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setTextColor(0xFFFFFFFF); // High visibility in dark theme
        input.setHintTextColor(0xFF556677);
        container.addView(input);

        // Helper text explaining setup options
        TextView helpText = new TextView(this);
        helpText.setText("\nCommon Options:\n" +
                "• Emulator: http://10.0.2.2:8080/\n" +
                "• Real Device: Your PC IP (e.g., http://192.168.1.5:8080/)\n\n" +
                "Make sure your backend server is running and the device is connected to the same WiFi network!");
        helpText.setTextSize(13);
        helpText.setTextColor(0xFF8899AA);
        container.addView(helpText);

        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newUrl = input.getText().toString().trim();
            if (newUrl.isEmpty()) {
                Toast.makeText(this, "URL cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                newUrl = "http://" + newUrl;
            }
            if (!newUrl.endsWith("/")) {
                newUrl += "/";
            }

            // Save to SharedPreferences
            SharedPreferences sp = getSharedPreferences("TruthLensPrefs", MODE_PRIVATE);
            sp.edit().putString("backend_url", newUrl).apply();

            // Update Retrofit Client
            RetrofitClient.updateBaseUrl(newUrl);

            // Update UI
            if (textCurrentServer != null) {
                textCurrentServer.setText("Current: " + newUrl);
            }

            Toast.makeText(this, "Server URL updated successfully!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
