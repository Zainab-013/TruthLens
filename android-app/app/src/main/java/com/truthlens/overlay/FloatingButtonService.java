package com.truthlens.overlay;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.truthlens.R;
import com.truthlens.ScreenCaptureActivity;

/**
 * FloatingButtonService
 *
 * Creates a draggable floating circle on screen.
 * - User activates it from MainActivity
 * - Circle floats over all apps
 * - Tap the circle → triggers screen capture + OCR
 * - Drag to move it around
 */
public class FloatingButtonService extends Service {

    private WindowManager windowManager;
    private View floatingView;

    private static final String CHANNEL_ID = "TruthLensOverlay";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification channel (required for foreground service)
        createNotificationChannel();

        // Start as foreground service
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TruthLens Active")
                .setContentText("Tap the floating button to capture text")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build();

        startForeground(1, notification);

        // Create the floating button
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Create a simple ImageView as the floating button
        floatingView = new ImageView(this);
        ((ImageView) floatingView).setImageResource(android.R.drawable.ic_menu_search);
        floatingView.setBackgroundResource(R.drawable.floating_circle);
        floatingView.setPadding(16, 16, 16, 16);

        // Layout parameters for overlay
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                150,  // width in pixels
                150,  // height in pixels
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 50;
        params.y = 200;

        // Add view to window
        windowManager.addView(floatingView, params);

        // Make it draggable + clickable
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isMoved = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isMoved = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);

                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isMoved = true;
                        }

                        params.x = initialX + deltaX;
                        params.y = initialY + deltaY;
                        windowManager.updateViewLayout(floatingView, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isMoved) {
                            // CLICK - trigger screen capture
                            onFloatingButtonClicked();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * Called when floating button is tapped.
     * Launches ScreenCaptureActivity to capture and analyze screen.
     */
    private void onFloatingButtonClicked() {
        Intent intent = new Intent(this, ScreenCaptureActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "TruthLens Overlay",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
