package com.truthlens;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * ResultActivity - Result Screen
 *
 * Displays the AI vs Human analysis results:
 * - AI Percentage (with progress bar)
 * - Human Percentage (with progress bar)
 * - Verdict
 * - Explanation list
 */
public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Get views
        TextView textVerdict = findViewById(R.id.textVerdict);
        TextView textAiPercentage = findViewById(R.id.textAiPercentage);
        TextView textHumanPercentage = findViewById(R.id.textHumanPercentage);
        ProgressBar progressAi = findViewById(R.id.progressAi);
        ProgressBar progressHuman = findViewById(R.id.progressHuman);
        TextView textExplanation = findViewById(R.id.textExplanation);
        Button btnBack = findViewById(R.id.btnBack);

        // Get data from intent
        double aiPercentage = getIntent().getDoubleExtra("ai_percentage", 0);
        double humanPercentage = getIntent().getDoubleExtra("human_percentage", 0);
        String verdict = getIntent().getStringExtra("verdict");
        String[] explanations = getIntent().getStringArrayExtra("explanation");

        // Display AI Percentage
        textAiPercentage.setText(String.format("%.1f%%", aiPercentage));
        progressAi.setProgress((int) aiPercentage);

        // Display Human Percentage
        textHumanPercentage.setText(String.format("%.1f%%", humanPercentage));
        progressHuman.setProgress((int) humanPercentage);

        // Display Verdict
        if (verdict != null) {
            textVerdict.setText(verdict);
        }

        // Display Explanations
        if (explanations != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < explanations.length; i++) {
                sb.append("• ").append(explanations[i]);
                if (i < explanations.length - 1) {
                    sb.append("\n\n");
                }
            }
            textExplanation.setText(sb.toString());
        }

        // Back button
        btnBack.setOnClickListener(v -> finish());
    }
}
