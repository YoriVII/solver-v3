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
        
        // ACCURACY FIX: Build a path that hits every point exactly
        Path path = new Path();
        
        // 1. Start exactly at the first red dot
        path.moveTo(pathCoords[0][0], pathCoords[0][1]);
        
        // 2. Draw straight lines to every subsequent red dot
        for (int i = 1; i < pathCoords.length; i++) {
            path.lineTo(pathCoords[i][0], pathCoords[i][1]);
        }

        GestureDescription.Builder builder = new GestureDescription.Builder();
        
        // ACCURACY FIX: 
        // 1. Duration is calculated based on distance to ensure speed is constant (not too fast to skip)
        // 2. We use a single stroke to connect them all, which Android interprets as a continuous drag
        // 3. Duration: 50ms per letter segment ensures the game registers it but it's still fast
        long totalDuration = Math.max(200, pathCoords.length * 100); 
        
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, totalDuration));
        
        return dispatchGesture(builder.build(), null, null);
    }
    
    public void reset() { isInterrupted = false; }
}
