package com.truthlens;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.truthlens.api.ApiService;
import com.truthlens.api.RetrofitClient;
import com.truthlens.model.AnalyzeRequest;
import com.truthlens.model.AnalyzeResponse;
import com.truthlens.model.HistoryItem;
import com.truthlens.utils.HistoryManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * TextSelectionActivity - Google Lens-style Text Selection with Handles
 *
 * 1. Automatically runs OCR on the entire screenshot at startup.
 * 2. Highlights selectable words with a subtle indicator overlay.
 * 3. Shows drag handles at start/end points of the selection.
 * 4. Selects text line-by-line horizontally in reading order based on handle positions.
 * 5. Extracts text and updates the UI dynamically in real-time.
 */
public class TextSelectionActivity extends AppCompatActivity {

    private ImageView imageView;
    private Button btnAnalyze;
    private Button btnRetake;
    private TextView textExtracted;
    private TextView textInstruction;
    private ProgressBar progressBar;

    private Bitmap screenshotBitmap;
    private Bitmap displayBitmap;

    // Detected words from full-screen OCR (in reading order)
    private List<Text.Element> detectedWords = new ArrayList<>();
    private boolean ocrComplete = false;

    // Selection coordinates (relative to image)
    private float startX, startY, endX, endY;
    private boolean isSelecting = false;
    private boolean isDraggingStartHandle = false;
    private boolean isDraggingEndHandle = false;
    private boolean hasSelection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_selection);

        imageView = findViewById(R.id.imageScreenshot);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        btnRetake = findViewById(R.id.btnRetake);
        textExtracted = findViewById(R.id.textExtracted);
        textInstruction = findViewById(R.id.textInstruction);
        progressBar = findViewById(R.id.progressBar);

        // Load screenshot
        String path = getIntent().getStringExtra("screenshot_path");
        if (path == null) {
            Toast.makeText(this, "No screenshot found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        screenshotBitmap = BitmapFactory.decodeFile(path);
        if (screenshotBitmap == null) {
            Toast.makeText(this, "Failed to load screenshot", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set initial bitmap display
        displayBitmap = screenshotBitmap.copy(Bitmap.Config.ARGB_8888, true);
        imageView.setImageBitmap(displayBitmap);

        // Run full-screen OCR at startup
        runFullOcr();

        // Touch listener for drawing selection rectangle and tap-selecting
        imageView.setOnTouchListener((v, event) -> {
            if (!ocrComplete) return false;

            float[] coords = getImageCoordinates(event.getX(), event.getY());
            float tx = coords[0];
            float ty = coords[1];

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (hasSelection) {
                        float density = getResources().getDisplayMetrics().density;
                        float hitRadius = 60f * density; // 60dp comfortable hit radius

                        // Calculate screen-space distances to both handles
                        float distStart = (float) Math.hypot(tx - startX, ty - startY) * scale;
                        float distEnd = (float) Math.hypot(tx - endX, ty - endY) * scale;

                        boolean nearStart = distStart < hitRadius;
                        boolean nearEnd = distEnd < hitRadius;

                        if (nearStart && nearEnd) {
                            // If close to both handles (e.g. small selection), select the closer one
                            if (distStart <= distEnd) {
                                isDraggingStartHandle = true;
                            } else {
                                isDraggingEndHandle = true;
                            }
                            isSelecting = false;
                        } else if (nearStart) {
                            isDraggingStartHandle = true;
                            isSelecting = false;
                        } else if (nearEnd) {
                            isDraggingEndHandle = true;
                            isSelecting = false;
                        } else {
                            // Start a new selection
                            startX = tx;
                            startY = ty;
                            endX = tx;
                            endY = ty;
                            isSelecting = true;
                            isDraggingStartHandle = false;
                            isDraggingEndHandle = false;
                            hasSelection = false;
                        }
                    } else {
                        // Start a new selection
                        startX = tx;
                        startY = ty;
                        endX = tx;
                        endY = ty;
                        isSelecting = true;
                        isDraggingStartHandle = false;
                        isDraggingEndHandle = false;
                        hasSelection = false;
                    }
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (isDraggingStartHandle) {
                        startX = tx;
                        startY = ty;
                        drawSelection();
                        updateSelectionText();
                    } else if (isDraggingEndHandle) {
                        endX = tx;
                        endY = ty;
                        drawSelection();
                        updateSelectionText();
                    } else if (isSelecting) {
                        endX = tx;
                        endY = ty;
                        drawSelection();
                        updateSelectionText();
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    if (isSelecting || isDraggingStartHandle || isDraggingEndHandle) {
                        boolean wasSelecting = isSelecting;
                        isSelecting = false;
                        isDraggingStartHandle = false;
                        isDraggingEndHandle = false;
                        hasSelection = true;

                        float dist = (float) Math.hypot(endX - startX, endY - startY);
                        if (dist < 15 && wasSelecting) { // Tapped
                            selectTappedWord(startX, startY);
                        } else {
                            // Snap handle positions to actual boundaries of start and end words
                            if (ocrComplete && !detectedWords.isEmpty()) {
                                int startWordIdx = findClosestWordIndex(startX, startY);
                                int endWordIdx = findClosestWordIndex(endX, endY);
                                if (startWordIdx != -1 && endWordIdx != -1) {
                                    int minIdx = Math.min(startWordIdx, endWordIdx);
                                    int maxIdx = Math.max(startWordIdx, endWordIdx);
                                    
                                    Rect startRect = detectedWords.get(minIdx).getBoundingBox();
                                    Rect endRect = detectedWords.get(maxIdx).getBoundingBox();
                                    if (startRect != null && endRect != null) {
                                        startX = startRect.left;
                                        startY = startRect.centerY();
                                        endX = endRect.right;
                                        endY = endRect.centerY();
                                    }
                                }
                            }
                            drawSelection();
                            updateSelectionText();
                        }
                    }
                    return true;
            }
            return false;
        });

        // Analyze extracted text
        btnAnalyze.setOnClickListener(v -> {
            String text = textExtracted.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "No text to analyze", Toast.LENGTH_SHORT).show();
                return;
            }
            analyzeText(text);
        });

        // Retake
        btnRetake.setOnClickListener(v -> finish());
    }

    /**
     * Run full-screen OCR scan on startup
     */
    private void runFullOcr() {
        progressBar.setVisibility(View.VISIBLE);
        textInstruction.setText("Scanning screen for text...");
        btnAnalyze.setVisibility(View.GONE);

        InputImage image = InputImage.fromBitmap(screenshotBitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    progressBar.setVisibility(View.GONE);
                    detectedWords.clear();

                    // Collect all lines across all text blocks
                    List<Text.Line> allLines = new ArrayList<>();
                    for (Text.TextBlock block : text.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            if (line.getBoundingBox() != null) {
                                allLines.add(line);
                            }
                        }
                    }

                    // Sort the lines in visual reading order (top-to-bottom, left-to-right)
                    java.util.Collections.sort(allLines, (l1, l2) -> {
                        Rect r1 = l1.getBoundingBox();
                        Rect r2 = l2.getBoundingBox();
                        if (r1 == null || r2 == null) return 0;

                        // Check if the lines overlap vertically
                        // We use a threshold of 50% of the minimum line height to determine if they are on the same line
                        int minHeight = Math.min(r1.height(), r2.height());
                        int yThreshold = minHeight / 2;

                        boolean sameLine = Math.abs(r1.centerY() - r2.centerY()) < yThreshold;
                        if (sameLine) {
                            return Integer.compare(r1.left, r2.left);
                        } else {
                            return Integer.compare(r1.top, r2.top);
                        }
                    });

                    // Add elements (words) from the sorted lines to detectedWords in correct order
                    for (Text.Line line : allLines) {
                        for (Text.Element element : line.getElements()) {
                            if (element.getBoundingBox() != null) {
                                detectedWords.add(element);
                            }
                        }
                    }

                    ocrComplete = true;
                    textInstruction.setText("Drag handles or tap on words to select text");
                    drawSelection(); // Redraw with detectable word outlines
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    textInstruction.setText("Scan failed: " + e.getMessage());
                    Toast.makeText(this, "OCR failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Convert touch view coordinates to image bitmap coordinates
     */
    private float[] getImageCoordinates(float touchX, float touchY) {
        float viewW = imageView.getWidth();
        float viewH = imageView.getHeight();
        float imgW = screenshotBitmap.getWidth();
        float imgH = screenshotBitmap.getHeight();

        float scaleX = viewW / imgW;
        float scaleY = viewH / imgH;
        scale = Math.min(scaleX, scaleY);

        float offsetX = (viewW - imgW * scale) / 2f;
        float offsetY = (viewH - imgH * scale) / 2f;

        float imgX = (touchX - offsetX) / scale;
        float imgY = (touchY - offsetY) / scale;

        imgX = Math.max(0, Math.min(imgX, imgW));
        imgY = Math.max(0, Math.min(imgY, imgH));

        return new float[]{imgX, imgY};
    }
    private float scale = 1f;

    /**
     * Check if touch is near the start selection handle
     */
    private boolean isNearStartHandle(float x, float y) {
        float dx = x - startX;
        float dy = y - startY;
        return Math.hypot(dx, dy) < 85; // 85 pixels hit radius
    }

    /**
     * Check if touch is near the end selection handle
     */
    private boolean isNearEndHandle(float x, float y) {
        float dx = x - endX;
        float dy = y - endY;
        return Math.hypot(dx, dy) < 85; // 85 pixels hit radius
    }

    /**
     * Find index of the word closest to given coordinates
     */
    private int findClosestWordIndex(float x, float y) {
        if (detectedWords.isEmpty()) return -1;

        int closestIdx = -1;
        float minDistance = Float.MAX_VALUE;

        for (int i = 0; i < detectedWords.size(); i++) {
            Text.Element element = detectedWords.get(i);
            Rect rect = element.getBoundingBox();
            if (rect != null) {
                float cx = rect.exactCenterX();
                float cy = rect.exactCenterY();
                float dist = (float) Math.hypot(x - cx, y - cy);
                if (dist < minDistance) {
                    minDistance = dist;
                    closestIdx = i;
                }
            }
        }
        return closestIdx;
    }

    /**
     * Retrieve indices of all words in the current selection range
     */
    private List<Integer> getSelectedWordIndices() {
        List<Integer> selectedIndices = new ArrayList<>();
        if (!ocrComplete || detectedWords.isEmpty()) return selectedIndices;

        int startIndex = findClosestWordIndex(startX, startY);
        int endIndex = findClosestWordIndex(endX, endY);

        if (startIndex != -1 && endIndex != -1) {
            int minIdx = Math.min(startIndex, endIndex);
            int maxIdx = Math.max(startIndex, endIndex);
            for (int i = minIdx; i <= maxIdx; i++) {
                selectedIndices.add(i);
            }
        }
        return selectedIndices;
    }

    /**
     * Draw Google Lens-style selection on the image bitmap with drag handles
     */
    private void drawSelection() {
        if (screenshotBitmap == null) return;

        displayBitmap = screenshotBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(displayBitmap);

        // Draw semi-transparent dim overlay on the whole image (makes text/highlighters pop)
        Paint dimPaint = new Paint();
        dimPaint.setColor(Color.parseColor("#4D000000")); // 30% opacity black
        canvas.drawRect(0, 0, displayBitmap.getWidth(), displayBitmap.getHeight(), dimPaint);

        // Subtle background overlay for all selectable words
        Paint wordPaint = new Paint();
        wordPaint.setColor(Color.parseColor("#15FFFFFF")); // Soft white overlay
        wordPaint.setStyle(Paint.Style.FILL);

        Paint wordBorderPaint = new Paint();
        wordBorderPaint.setColor(Color.parseColor("#20FFFFFF")); // Soft border
        wordBorderPaint.setStyle(Paint.Style.STROKE);
        wordBorderPaint.setStrokeWidth(2);

        for (Text.Element element : detectedWords) {
            Rect elemRect = element.getBoundingBox();
            if (elemRect != null) {
                canvas.drawRect(elemRect, wordPaint);
                canvas.drawRect(elemRect, wordBorderPaint);
            }
        }

        // Highlight selected words and draw selection handles
        if (isSelecting || isDraggingStartHandle || isDraggingEndHandle || hasSelection) {
            List<Integer> selectedIndices = getSelectedWordIndices();

            // Selection highlighter (Google Lens blue)
            Paint selectWordPaint = new Paint();
            selectWordPaint.setColor(Color.parseColor("#6600B0FF")); // 40% blue selection
            selectWordPaint.setStyle(Paint.Style.FILL);

            for (int idx : selectedIndices) {
                Text.Element element = detectedWords.get(idx);
                Rect elemRect = element.getBoundingBox();
                if (elemRect != null) {
                    RectF elemRectF = new RectF(elemRect);
                    // Clear the dim overlay underneath this word
                    canvas.drawBitmap(screenshotBitmap, elemRect, elemRectF, null);
                    // Overlay the selection highlight
                    canvas.drawRect(elemRectF, selectWordPaint);
                }
            }

            // Draw start and end selection handles (pins)
            Paint handlePaint = new Paint();
            handlePaint.setColor(Color.parseColor("#00B0FF")); // Bright selection blue
            handlePaint.setStyle(Paint.Style.FILL);
            handlePaint.setAntiAlias(true);

            Paint handleGlowPaint = new Paint();
            handleGlowPaint.setColor(Color.parseColor("#4000B0FF")); // Translucent outer ring
            handleGlowPaint.setStyle(Paint.Style.FILL);
            handleGlowPaint.setAntiAlias(true);

            // Compute density-aware handle and glow radii in image coordinates
            float density = getResources().getDisplayMetrics().density;
            float s = (scale > 0) ? scale : 1f;
            float handleRadius = (12f * density) / s;
            float glowRadius = (22f * density) / s;

            // Start handle
            canvas.drawCircle(startX, startY, glowRadius, handleGlowPaint);
            canvas.drawCircle(startX, startY, handleRadius, handlePaint);

            // End handle
            canvas.drawCircle(endX, endY, glowRadius, handleGlowPaint);
            canvas.drawCircle(endX, endY, handleRadius, handlePaint);
        }

        imageView.setImageBitmap(displayBitmap);
    }

    /**
     * Taps a word to select it directly
     */
    private void selectTappedWord(float x, float y) {
        if (detectedWords.isEmpty()) return;

        int closestIdx = findClosestWordIndex(x, y);
        if (closestIdx != -1) {
            Text.Element element = detectedWords.get(closestIdx);
            Rect rect = element.getBoundingBox();
            if (rect != null) {
                RectF elemRect = new RectF(rect);
                
                // Get distance in screen pixels
                float density = getResources().getDisplayMetrics().density;
                float maxTapDistance = 48f * density; // 48dp search radius for tap
                
                // Calculate distance from (x, y) to the nearest point on the rect
                float dx = Math.max(elemRect.left - x, Math.max(0f, x - elemRect.right));
                float dy = Math.max(elemRect.top - y, Math.max(0f, y - elemRect.bottom));
                float distScreen = (float) Math.hypot(dx, dy) * scale;
                
                if (distScreen < maxTapDistance) {
                    startX = elemRect.left;
                    startY = elemRect.centerY();
                    endX = elemRect.right;
                    endY = elemRect.centerY();
                    hasSelection = true;
                    drawSelection();
                    updateSelectionText();
                    return;
                }
            }
        }

        // If tap outside words, clear selection
        hasSelection = false;
        drawSelection();
        textExtracted.setVisibility(View.GONE);
        btnAnalyze.setVisibility(View.GONE);
        textInstruction.setText("Drag handles or tap on words to select text");
        textInstruction.setBackgroundColor(Color.parseColor("#E94560"));
    }

    /**
     * Compute and extract text contained in selection bounds, update instruction bar status
     */
    private void updateSelectionText() {
        if (!ocrComplete) return;

        List<Integer> selectedIndices = getSelectedWordIndices();
        StringBuilder sb = new StringBuilder();
        int wordCount = 0;

        for (int idx : selectedIndices) {
            Text.Element element = detectedWords.get(idx);
            sb.append(element.getText()).append(" ");
            wordCount++;
        }

        String extracted = sb.toString().trim();
        if (extracted.isEmpty()) {
            textExtracted.setVisibility(View.GONE);
            btnAnalyze.setVisibility(View.GONE);
            textInstruction.setText("Drag handles or tap on words to select text");
            textInstruction.setBackgroundColor(Color.parseColor("#E94560")); // Reset to red
        } else {
            textExtracted.setText(extracted);
            textExtracted.setVisibility(View.VISIBLE);

            if (wordCount >= 20) {
                textInstruction.setText("Ready to analyze! Selected " + wordCount + " words.");
                textInstruction.setBackgroundColor(Color.parseColor("#2E7D32")); // Green success
                btnAnalyze.setVisibility(View.VISIBLE);
            } else {
                textInstruction.setText("Selected " + wordCount + " words (need at least 20).");
                textInstruction.setBackgroundColor(Color.parseColor("#E94560")); // Red alert
                btnAnalyze.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Send extracted text to the backend API for verification
     */
    private void analyzeText(String text) {
        progressBar.setVisibility(View.VISIBLE);
        textInstruction.setText("Analyzing text legitimacy...");

        ApiService api = RetrofitClient.getApiService();
        api.analyzeText(new AnalyzeRequest(text)).enqueue(new Callback<AnalyzeResponse>() {
            @Override
            public void onResponse(Call<AnalyzeResponse> call, Response<AnalyzeResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    AnalyzeResponse r = response.body();
                    
                    // Save to local cache history
                    HistoryManager.saveHistoryItem(TextSelectionActivity.this, 
                        new HistoryItem(
                            text, 
                            r.getAi_percentage(), 
                            r.getHuman_percentage(), 
                            r.getVerdict(), 
                            r.getExplanation()
                        )
                    );
                    
                    Intent intent = new Intent(TextSelectionActivity.this, ResultActivity.class);
                    intent.putExtra("ai_percentage", r.getAi_percentage());
                    intent.putExtra("human_percentage", r.getHuman_percentage());
                    intent.putExtra("verdict", r.getVerdict());
                    intent.putExtra("explanation", r.getExplanation().toArray(new String[0]));
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(TextSelectionActivity.this, "Analysis failed", Toast.LENGTH_SHORT).show();
                    textInstruction.setText("Ready to analyze!");
                }
            }

            @Override
            public void onFailure(Call<AnalyzeResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TextSelectionActivity.this, "Connection failed: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
                textInstruction.setText("Ready to analyze!");
            }
        });
    }
}
