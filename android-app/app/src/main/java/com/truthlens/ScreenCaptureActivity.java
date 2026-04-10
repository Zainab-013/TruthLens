package com.truthlens;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.truthlens.api.ApiService;
import com.truthlens.api.RetrofitClient;
import com.truthlens.model.AnalyzeRequest;
import com.truthlens.model.AnalyzeResponse;

import java.nio.ByteBuffer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ScreenCaptureActivity
 *
 * Handles the screen capture flow:
 * 1. Request MediaProjection permission
 * 2. Capture screenshot
 * 3. Run OCR (Google ML Kit) to extract text
 * 4. Send text to API for analysis
 * 5. Open ResultActivity with results
 */
public class ScreenCaptureActivity extends Activity {

    private static final int REQUEST_MEDIA_PROJECTION = 1001;
    private MediaProjectionManager projectionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Request screen capture permission
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                // Small delay to let the permission dialog close
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    captureScreen(resultCode, data);
                }, 500);
            } else {
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void captureScreen(int resultCode, Intent data) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = metrics.densityDpi;

        ImageReader imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2);

        MediaProjection projection = projectionManager.getMediaProjection(resultCode, data);

        VirtualDisplay virtualDisplay = projection.createVirtualDisplay(
                "TruthLensCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null, null
        );

        // Wait a moment for the image to be ready
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Image image = imageReader.acquireLatestImage();

            if (image != null) {
                // Convert Image to Bitmap
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * width;

                Bitmap bitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride, height,
                        Bitmap.Config.ARGB_8888
                );
                bitmap.copyPixelsFromBuffer(buffer);

                // Crop to actual screen size
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);

                image.close();
                virtualDisplay.release();
                projection.stop();
                imageReader.close();

                // Run OCR on the bitmap
                runOCR(bitmap);

            } else {
                Toast.makeText(this, "Failed to capture screen", Toast.LENGTH_SHORT).show();
                virtualDisplay.release();
                projection.stop();
                imageReader.close();
                finish();
            }
        }, 1000);
    }

    /**
     * Extract text from screenshot using Google ML Kit OCR
     */
    private void runOCR(Bitmap bitmap) {
        Toast.makeText(this, "Extracting text...", Toast.LENGTH_SHORT).show();

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    String extractedText = text.getText();

                    if (extractedText.isEmpty()) {
                        Toast.makeText(this, "No text found on screen", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    // Check word count
                    int wordCount = extractedText.trim().split("\\s+").length;
                    if (wordCount < 50) {
                        Toast.makeText(this,
                                "Only " + wordCount + " words found. Need at least 50 words.",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    // Send to API
                    analyzeText(extractedText);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "OCR failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    /**
     * Send extracted text to Spring Boot API
     */
    private void analyzeText(String text) {
        Toast.makeText(this, "Analyzing text...", Toast.LENGTH_SHORT).show();

        ApiService apiService = RetrofitClient.getApiService();
        AnalyzeRequest request = new AnalyzeRequest(text);

        apiService.analyzeText(request).enqueue(new Callback<AnalyzeResponse>() {
            @Override
            public void onResponse(Call<AnalyzeResponse> call, Response<AnalyzeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AnalyzeResponse result = response.body();

                    Intent intent = new Intent(ScreenCaptureActivity.this, ResultActivity.class);
                    intent.putExtra("ai_percentage", result.getAi_percentage());
                    intent.putExtra("human_percentage", result.getHuman_percentage());
                    intent.putExtra("verdict", result.getVerdict());
                    String[] explanations = result.getExplanation().toArray(new String[0]);
                    intent.putExtra("explanation", explanations);
                    startActivity(intent);
                } else {
                    Toast.makeText(ScreenCaptureActivity.this,
                            "Analysis failed", Toast.LENGTH_SHORT).show();
                }
                finish();
            }

            @Override
            public void onFailure(Call<AnalyzeResponse> call, Throwable t) {
                Toast.makeText(ScreenCaptureActivity.this,
                        "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}
