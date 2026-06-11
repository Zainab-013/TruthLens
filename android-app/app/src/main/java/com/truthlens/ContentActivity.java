package com.truthlens;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * ContentActivity - Displays legal or support content natively inside the application.
 */
public class ContentActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView txtContentTitle;
    private TextView txtContentBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        // Bind views
        btnBack = findViewById(R.id.btnBack);
        txtContentTitle = findViewById(R.id.txtContentTitle);
        txtContentBody = findViewById(R.id.txtContentBody);

        // Configure back button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Load content based on requested type
        String contentType = getIntent().getStringExtra("type");
        if (contentType == null) {
            contentType = "terms";
        }

        loadContent(contentType);
    }

    private void loadContent(String type) {
        String title;
        StringBuilder body = new StringBuilder();

        switch (type.toLowerCase()) {
            case "privacy":
                title = "Privacy Policy";
                body.append("Effective Date: June 11, 2026\n\n")
                    .append("Welcome to TruthLens. We value your trust and are committed to protecting your privacy.\n\n")
                    .append("1. Information We Collect\n")
                    .append("TruthLens is a text verification tool. When you submit text for analysis, the text is securely transmitted to our backend NLP server to calculate perplexity, burstiness, vocabulary richness, and repetition metrics.\n\n")
                    .append("2. Data Storage and Retention\n")
                    .append("We do NOT store or save the texts you submit on our servers. All linguistic measurements are processed in memory and returned immediately. The history items are saved locally in your device's private storage (SharedPreferences) and are never shared with us.\n\n")
                    .append("3. No Personal Data Sharing\n")
                    .append("We do not collect personal identification information, email addresses, phone numbers, or device identifiers. We do not sell, trade, or share any user data with third parties.\n\n")
                    .append("4. Policy Updates\n")
                    .append("We may update this policy periodically. Continued use of the app constitutes agreement to any changes.\n\n")
                    .append("Thank you for choosing TruthLens!");
                break;

            case "support":
                title = "Support & Contact";
                body.append("Help Center\n\n")
                    .append("Thank you for using TruthLens. If you encounter any technical issues, have questions about analysis metrics, or would like to share feedback, we are here to help.\n\n")
                    .append("1. Contact Information\n")
                    .append("• Email Support: shaikhyasmeen78600@gmail.com\n")
                    .append("• Website: https://truthlens-z.netlify.app\n")
                    .append("• Development Team: Shaikh Zainab Sattar\n\n")
                    .append("2. Common Issues & Troubleshooting\n")
                    .append("• Network Errors: If you receive a connection failure warning, make sure your mobile device is connected to the same local WiFi network as the API gateway server (configured in your client settings).\n")
                    .append("• Minimum Words constraint: The NLP model calculations require a text of at least 20 words to accurately calibrate burstiness and vocabulary richness. Shorter text inputs will display a prompt to add more words.\n\n")
                    .append("We aim to respond to all inquiries within 24-48 business hours.");
                break;

            case "terms":
            default:
                title = "Terms of Service";
                body.append("Last Updated: June 11, 2026\n\n")
                    .append("Please read these Terms of Service carefully before using the TruthLens mobile application.\n\n")
                    .append("1. Acceptance of Terms\n")
                    .append("By installing and opening this application, you agree to be bound by these Terms of Service. If you do not agree, please do not use the application.\n\n")
                    .append("2. Permitted Use\n")
                    .append("TruthLens is intended to help you detect whether text content is likely human-written or AI-generated. You agree to use the application solely for lawful purposes and in accordance with these terms.\n\n")
                    .append("3. Disclaimer of Accuracy\n")
                    .append("Our service calculates probability scores based on custom GPT-2 linguistic metrics. While we strive for reliability, AI detection is probabilistic. TruthLens does not guarantee absolute accuracy, and results should not be used as the sole basis for critical decisions (e.g. academic grading, legal actions, or employment).\n\n")
                    .append("4. Limitation of Liability\n")
                    .append("In no event shall TruthLens be liable for any direct, indirect, or incidental damages arising out of your use or inability to use the service.");
                break;
        }

        if (txtContentTitle != null) {
            txtContentTitle.setText(title);
        }
        if (txtContentBody != null) {
            txtContentBody.setText(body.toString());
        }
    }
}
