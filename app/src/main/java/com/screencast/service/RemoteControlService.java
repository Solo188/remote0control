package com.screencast.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.RequiresApi;

public class RemoteControlService extends AccessibilityService {
    private static final String TAG = "RemoteControlService";
    private static RemoteControlService instance;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "RemoteControlService connected");
    }

    public static RemoteControlService getInstance() {
        return instance;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void performClick(float xRatio, float yRatio, int screenWidth, int screenHeight) {
        float x = xRatio * screenWidth;
        float y = yRatio * screenHeight;

        Path clickPath = new Path();
        clickPath.moveTo(x, y);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(clickPath, 0, 100);

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();

        dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                Log.d(TAG, "Click performed at (" + x + ", " + y + ")");
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                Log.w(TAG, "Click cancelled");
            }
        }, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void performSwipe(float x1Ratio, float y1Ratio, float x2Ratio, float y2Ratio,
                             int screenWidth, int screenHeight) {
        float x1 = x1Ratio * screenWidth;
        float y1 = y1Ratio * screenHeight;
        float x2 = x2Ratio * screenWidth;
        float y2 = y2Ratio * screenHeight;

        Path swipePath = new Path();
        swipePath.moveTo(x1, y1);
        swipePath.lineTo(x2, y2);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(swipePath, 0, 500);

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();

        dispatchGesture(gesture, null, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void performLongClick(float xRatio, float yRatio, int screenWidth, int screenHeight) {
        float x = xRatio * screenWidth;
        float y = yRatio * screenHeight;

        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 800);

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();

        dispatchGesture(gesture, null, null);
    }

    public void performBack() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    public void performHome() {
        performGlobalAction(GLOBAL_ACTION_HOME);
    }

    public void performRecents() {
        performGlobalAction(GLOBAL_ACTION_RECENTS);
    }
}
