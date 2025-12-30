package com.wordscapes.helper;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class OverlayService extends Service {
    private WindowManager wm;
    private View overlayView;
    private Trie trie;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        trie = new Trie(); // Load the brain

        // Setup the layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#CC000000")); // Semi-transparent black
        layout.setPadding(20, 20, 20, 20);

        // 1. Input Field (Type letters here)
        EditText input = new EditText(this);
        input.setHint("TYPE LETTERS (e.g. ACT)");
        input.setHintTextColor(Color.LTGRAY);
        input.setTextColor(Color.WHITE);
        input.setTextSize(18);
        layout.addView(input);

        // 2. Solve Button
        Button solveBtn = new Button(this);
        solveBtn.setText("SOLVE");
        solveBtn.setBackgroundColor(Color.parseColor("#00BCD4")); // Cyan
        solveBtn.setTextColor(Color.WHITE);
        layout.addView(solveBtn);

        // 3. Results View
        TextView resultsView = new TextView(this);
        resultsView.setTextColor(Color.GREEN);
        resultsView.setTextSize(16);
        resultsView.setPadding(0, 10, 0, 0);
        layout.addView(resultsView);

        // Button Logic
        solveBtn.setOnClickListener(v -> {
            String letters = input.getText().toString();
            if (letters.isEmpty()) return;
            
            List<String> found = trie.solve(letters);
            if (found.isEmpty()) {
                resultsView.setText("No words found (Try: CAT)");
            } else {
                resultsView.setText("Found: " + found.toString());
            }
        });

        // Window Params (Floating on top)
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            600, // Width
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT);
        
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = 200;

        wm.addView(layout, params);
        overlayView = layout;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) wm.removeView(overlayView);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
