package com.truthlens;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

/**
 * SplashActivity - Entry point splash screen.
 * Displays logo and routes the user after 2 seconds based on terms acceptance.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Transition after 2000ms delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean termsAccepted = getSharedPreferences("TruthLensPrefs", MODE_PRIVATE)
                    .getBoolean("terms_accepted", false);

            Intent intent;
            if (termsAccepted) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, AboutActivity.class);
                intent.putExtra("is_onboarding", true);
            }
            startActivity(intent);
            finish(); // Finish SplashActivity so the user cannot back-navigate to it
        }, 2000);
    }
}
