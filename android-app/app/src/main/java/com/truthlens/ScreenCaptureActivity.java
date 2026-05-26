package com.truthlens;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

/**
 * ScreenCaptureActivity
 *
 * 1. Requests screen capture permission
 * 2. Captures screenshot
 * 3. Saves to file
 * 4. Opens TextSelectionActivity for user to select text region
 */
public class ScreenCaptureActivity extends Activity {

    private static final int REQUEST_CAPTURE = 1001;
    private MediaProjectionManager projManager;
    private VirtualDisplay virtualDisplay;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            projManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(projManager.createScreenCaptureIntent(), REQUEST_CAPTURE);
        } catch (Exception e) {
            Toast.makeText(this, "Screen capture not available: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CAPTURE && resultCode == RESULT_OK && data != null) {
            // Small delay to let permission dialog close
            new Handler(Looper.getMainLooper()).postDelayed(() -> captureScreen(resultCode, data), 500);
        } else {
            Toast.makeText(this, "Screen capture cancelled", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void captureScreen(int resultCode, Intent data) {
        try {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            int density = metrics.densityDpi;

            ImageReader reader = ImageReader.newInstance(width, height,
                    android.graphics.PixelFormat.RGBA_8888, 2);

            // Upgrade service to MEDIA_PROJECTION type BEFORE obtaining projection token on Android 10+
            Intent upgradeIntent = new Intent(this, com.truthlens.overlay.FloatingButtonService.class);
            upgradeIntent.setAction(com.truthlens.overlay.FloatingButtonService.ACTION_START_PROJECTION);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(upgradeIntent);
            } else {
                startService(upgradeIntent);
            }

            // Introduce a 200ms delay to let the service transition to MEDIA_PROJECTION type
            // to avoid a SecurityException race condition on Android 14+
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    MediaProjection projection = projManager.getMediaProjection(resultCode, data);
                    if (projection == null) {
                        Toast.makeText(this, "Failed to get MediaProjection", Toast.LENGTH_SHORT).show();
                        // Downgrade service back to standard overlay
                        Intent downgradeIntent = new Intent(this, com.truthlens.overlay.FloatingButtonService.class);
                        downgradeIntent.setAction(com.truthlens.overlay.FloatingButtonService.ACTION_STOP_PROJECTION);
                        startService(downgradeIntent);
                        reader.close();
                        finish();
                        return;
                    }

                    // Register callback to comply with Android 14+ requirements
                    projection.registerCallback(new MediaProjection.Callback() {
                        @Override
                        public void onStop() {
                            super.onStop();
                        }
                    }, new Handler(Looper.getMainLooper()));

                    reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                        private boolean captured = false;

                        @Override
                        public void onImageAvailable(ImageReader reader) {
                            if (captured) return;

                            Image image = null;
                            try {
                                image = reader.acquireNextImage();
                                if (image != null) {
                                    captured = true;
                                    if (timeoutRunnable != null) {
                                        timeoutHandler.removeCallbacks(timeoutRunnable);
                                        timeoutRunnable = null;
                                    }

                                    // Convert to bitmap
                                    Image.Plane[] planes = image.getPlanes();
                                    ByteBuffer buffer = planes[0].getBuffer();
                                    int pixelStride = planes[0].getPixelStride();
                                    int rowStride = planes[0].getRowStride();
                                    int rowPadding = rowStride - pixelStride * width;

                                    Bitmap bitmap = Bitmap.createBitmap(
                                            width + rowPadding / pixelStride, height,
                                            Bitmap.Config.ARGB_8888);
                                    bitmap.copyPixelsFromBuffer(buffer);
                                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);

                                    image.close();
                                    if (virtualDisplay != null) {
                                        virtualDisplay.release();
                                        virtualDisplay = null;
                                    }
                                    projection.stop();
                                    reader.close();

                                    // Downgrade service back to standard overlay type
                                    Intent downgradeIntent = new Intent(ScreenCaptureActivity.this, com.truthlens.overlay.FloatingButtonService.class);
                                    downgradeIntent.setAction(com.truthlens.overlay.FloatingButtonService.ACTION_STOP_PROJECTION);
                                    startService(downgradeIntent);

                                    // Save bitmap to temp file
                                    File file = new File(getCacheDir(), "screenshot.png");
                                    FileOutputStream fos = new FileOutputStream(file);
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                                    fos.close();

                                    // Open TextSelectionActivity
                                    Intent intent = new Intent(ScreenCaptureActivity.this, TextSelectionActivity.class);
                                    intent.putExtra("screenshot_path", file.getAbsolutePath());
                                    startActivity(intent);
                                    finish();
                                }
                            } catch (Exception e) {
                                Toast.makeText(ScreenCaptureActivity.this, "Capture error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                if (timeoutRunnable != null) {
                                    timeoutHandler.removeCallbacks(timeoutRunnable);
                                    timeoutRunnable = null;
                                }
                                if (image != null) {
                                    try { image.close(); } catch (Exception ignored) {}
                                }
                                if (virtualDisplay != null) {
                                     try { virtualDisplay.release(); } catch (Exception ignored) {}
                                     virtualDisplay = null;
                                }
                                try { projection.stop(); } catch (Exception ignored) {}
                                try { reader.close(); } catch (Exception ignored) {}

                                // Downgrade service back to standard overlay type
                                Intent downgradeIntent = new Intent(ScreenCaptureActivity.this, com.truthlens.overlay.FloatingButtonService.class);
                                downgradeIntent.setAction(com.truthlens.overlay.FloatingButtonService.ACTION_STOP_PROJECTION);
                                startService(downgradeIntent);

                                finish();
                            }
                        }
                    }, new Handler(Looper.getMainLooper()));

                    // Now start the virtual display AFTER the listener is set
                    virtualDisplay = projection.createVirtualDisplay(
                            "TruthLens", width, height, density,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                            reader.getSurface(), null, null);

                    timeoutRunnable = new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ScreenCaptureActivity.this, "Screen capture timed out. Please try again.", Toast.LENGTH_LONG).show();
                            if (virtualDisplay != null) {
                                try { virtualDisplay.release(); } catch (Exception ignored) {}
                                virtualDisplay = null;
                            }
                            try { projection.stop(); } catch (Exception ignored) {}
                            try { reader.close(); } catch (Exception ignored) {}

                            // Downgrade service back to standard overlay type
                            Intent downgradeIntent = new Intent(ScreenCaptureActivity.this, com.truthlens.overlay.FloatingButtonService.class);
                            downgradeIntent.setAction(com.truthlens.overlay.FloatingButtonService.ACTION_STOP_PROJECTION);
                            startService(downgradeIntent);

                            finish();
                        }
                    };
                    timeoutHandler.postDelayed(timeoutRunnable, 3000);

                } catch (Exception e) {
                    Toast.makeText(this, "Screenshot error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Downgrade service back to standard overlay type
                    Intent downgradeIntent = new Intent(this, com.truthlens.overlay.FloatingButtonService.class);
                    downgradeIntent.setAction(com.truthlens.overlay.FloatingButtonService.ACTION_STOP_PROJECTION);
                    startService(downgradeIntent);
                    try { reader.close(); } catch (Exception ignored) {}
                    finish();
                }
            }, 200);

        } catch (Exception e) {
            Toast.makeText(this, "Screenshot error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Downgrade service back to standard overlay type
            Intent downgradeIntent = new Intent(this, com.truthlens.overlay.FloatingButtonService.class);
            downgradeIntent.setAction(com.truthlens.overlay.FloatingButtonService.ACTION_STOP_PROJECTION);
            startService(downgradeIntent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }
}
