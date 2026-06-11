package com.truthlens;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * AboutActivity - About Us & Terms Consent Screen
 * Displays project overview, clickable footer links, and manages terms acceptance onboarding.
 */
public class AboutActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Button btnAccept;
    private TextView txtTerms;
    private TextView txtPrivacyPolicy;
    private TextView txtSupport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Bind views
        btnBack = findViewById(R.id.btnBack);
        btnAccept = findViewById(R.id.btnAccept);
        txtTerms = findViewById(R.id.txtTerms);
        txtPrivacyPolicy = findViewById(R.id.txtPrivacyPolicy);
        txtSupport = findViewById(R.id.txtSupport);

        // Check onboarding mode
        boolean isOnboarding = getIntent().getBooleanExtra("is_onboarding", false);

        if (isOnboarding) {
            // Hide back arrow, show Accept & Continue button
            if (btnBack != null) {
                btnBack.setVisibility(View.GONE);
            }
            if (btnAccept != null) {
                btnAccept.setVisibility(View.VISIBLE);
                btnAccept.setOnClickListener(v -> acceptTermsAndProceed());
            }
        } else {
            // Normal mode: Show back arrow, hide Accept & Continue button
            if (btnBack != null) {
                btnBack.setVisibility(View.VISIBLE);
                btnBack.setOnClickListener(v -> finish());
            }
            if (btnAccept != null) {
                btnAccept.setVisibility(View.GONE);
            }
        }

        // Click listeners for footer links
        if (txtTerms != null) {
            txtTerms.setOnClickListener(v -> openContentPage("terms"));
        }

        if (txtPrivacyPolicy != null) {
            txtPrivacyPolicy.setOnClickListener(v -> openContentPage("privacy"));
        }

        if (txtSupport != null) {
            txtSupport.setOnClickListener(v -> openContentPage("support"));
        }
    }

    /**
     * Persists terms acceptance in SharedPreferences and routes to the MainActivity.
     */
    private void acceptTermsAndProceed() {
        getSharedPreferences("TruthLensPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("terms_accepted", true)
                .apply();

        Intent intent = new Intent(AboutActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Opens ContentActivity to display text natively inside the app.
     */
    private void openContentPage(String type) {
        try {
            Intent intent = new Intent(this, ContentActivity.class);
            intent.putExtra("type", type);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open document: " + type, Toast.LENGTH_LONG).show();
        }
    }
}
