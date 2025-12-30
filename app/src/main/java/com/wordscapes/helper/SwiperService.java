package com.wordscapes.helper;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;

public class SwiperService extends AccessibilityService {
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}

    public void swipeWord(float[][] coords) {
        if (coords.length < 2) return;
        Path path = new Path();
        path.moveTo(coords[0][0], coords[0][1]);
        for (int i = 1; i < coords.length; i++) {
            path.lineTo(coords[i][0], coords[i][1]);
        }
        GestureDescription.Builder gb = new GestureDescription.Builder();
        gb.addStroke(new GestureDescription.StrokeDescription(path, 0, 400));
        dispatchGesture(gb.build(), null, null);
    }
}
