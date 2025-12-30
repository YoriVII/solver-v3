package com.wordscapes.helper;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;

public class SwiperService extends AccessibilityService {
    public static SwiperService instance;
    private boolean isInterrupted = false;

    @Override
    public void onServiceConnected() { instance = this; }
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override
    public void onInterrupt() { isInterrupted = true; }

    public void stopSwiping() {
        isInterrupted = true;
    }

    public boolean swipe(float[][] pathCoords) {
        if (isInterrupted || pathCoords == null || pathCoords.length < 2) return false;
        
        Path path = new Path();
        path.moveTo(pathCoords[0][0], pathCoords[0][1]);
        for (int i = 1; i < pathCoords.length; i++) {
            path.lineTo(pathCoords[i][0], pathCoords[i][1]);
        }

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 300));
        return dispatchGesture(builder.build(), null, null);
    }
    
    public void reset() { isInterrupted = false; }
}
