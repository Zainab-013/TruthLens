package com.truthlens.overlay;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.truthlens.MainActivity;
import com.truthlens.ScreenCaptureActivity;
import com.truthlens.R;

/**
 * FloatingButtonService - Compatible with ALL Android versions (API 24+)
 */
public class FloatingButtonService extends Service {

    public static boolean isRunning = false;
    private static final String TAG = "FloatingBtn";
    private static final String CHANNEL_ID = "truthlens_overlay";
    
    public static final String ACTION_START_PROJECTION = "com.truthlens.ACTION_START_PROJECTION";
    public static final String ACTION_STOP_PROJECTION = "com.truthlens.ACTION_STOP_PROJECTION";

    private WindowManager windowManager;
    private View floatingView;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_PROJECTION.equals(action)) {
                upgradeToMediaProjection();
            } else if (ACTION_STOP_PROJECTION.equals(action)) {
                downgradeToOverlay();
            }
        }
        return START_STICKY;
    }

    private void startForegroundWithOverlayType() {
        Notification notification = getOverlayNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            // Android 14+ starts with specialUse for overlay button
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            // Under Android 14, no type parameter needed for normal overlay FGS
            startForeground(1, notification);
        }
    }

    private void upgradeToMediaProjection() {
        Log.d(TAG, "Upgrading foreground service to MEDIA_PROJECTION type");
        Notification notification = getOverlayNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            // Android 14+ needs BOTH specialUse and mediaProjection type
            startForeground(1, notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE | 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else if (Build.VERSION.SDK_INT >= 29) {
            // Android 10+ needs mediaProjection type
            startForeground(1, notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(1, notification);
        }
    }

    private void downgradeToOverlay() {
        Log.d(TAG, "Downgrading foreground service back to overlay type");
        startForegroundWithOverlayType();
    }

    private Notification getOverlayNotification() {
        createNotificationChannel();

        Intent notifIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notifIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TruthLens")
                .setContentText("Tap floating TL button to scan text")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;

        // Double check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Log.e(TAG, "No overlay permission");
                Toast.makeText(this, "Overlay permission not granted", Toast.LENGTH_LONG).show();
                stopSelf();
                return;
            }
        }

        // Start as foreground service (required on Android 8+)
        try {
            startForegroundWithOverlayType();
        } catch (Exception e) {
            Log.e(TAG, "Foreground failed: " + e.getMessage(), e);
            // Try without foreground as last resort
            try {
                Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("TruthLens Active")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .build();
                startForeground(1, n);
            } catch (Exception e2) {
                Log.e(TAG, "Fallback foreground also failed", e2);
                stopSelf();
                return;
            }
        }

        // Create the floating button
        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (windowManager == null) {
                Log.e(TAG, "WindowManager is null");
                stopSelf();
                return;
            }

            // Red circle with "TL"
            TextView btn = new TextView(this);
            btn.setText("TL");
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            btn.setTypeface(null, Typeface.BOLD);
            btn.setGravity(Gravity.CENTER);
            btn.setBackgroundResource(R.drawable.floating_circle);
            floatingView = btn;

            int layoutFlag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;

            int sizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60,
                    getResources().getDisplayMetrics());

            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    sizePx, sizePx, layoutFlag,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 100;
            params.y = 400;

            windowManager.addView(floatingView, params);
            Log.d(TAG, "Button added to screen successfully");

            // Touch listener
            floatingView.setOnTouchListener(new View.OnTouchListener() {
                private int iX, iY;
                private float tX, tY;
                private boolean moved;
                private long t0;

                @Override
                public boolean onTouch(View v, MotionEvent e) {
                    switch (e.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            iX = params.x; iY = params.y;
                            tX = e.getRawX(); tY = e.getRawY();
                            moved = false; t0 = System.currentTimeMillis();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            int dx = (int)(e.getRawX()-tX), dy = (int)(e.getRawY()-tY);
                            if (Math.abs(dx)>10||Math.abs(dy)>10) moved = true;
                            params.x = iX+dx; params.y = iY+dy;
                            try { windowManager.updateViewLayout(floatingView, params); }
                            catch (Exception ignored) {}
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (!moved && System.currentTimeMillis()-t0 < 500) {
                                onButtonClicked();
                            }
                            return true;
                    }
                    return false;
                }
            });

        } catch (SecurityException se) {
            Log.e(TAG, "Security: " + se.getMessage());
            Toast.makeText(this, "Please enable overlay permission in Settings", Toast.LENGTH_LONG).show();
            stopSelf();
        } catch (Exception e) {
            Log.e(TAG, "Overlay failed: " + e.getMessage(), e);
            Toast.makeText(this, "Overlay not available: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private void onButtonClicked() {
        try {
            Intent i = new Intent(this, ScreenCaptureActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Log.e(TAG, "Click error: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (floatingView != null && windowManager != null) {
            try { windowManager.removeView(floatingView); } catch (Exception ignored) {}
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "TruthLens Overlay", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Keeps floating button active");
            NotificationManager mgr = getSystemService(NotificationManager.class);
            if (mgr != null) mgr.createNotificationChannel(ch);
        }
    }
}
