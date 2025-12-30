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
    private boolean isLoaded = false;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        trie = new Trie();
        
        // Load the huge dictionary in the background
        new Thread(() -> {
            trie.loadDictionary(this);
            isLoaded = true;
        }).start();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#E6212121")); // Dark Grey Background
        layout.setPadding(30, 30, 30, 30);

        EditText input = new EditText(this);
        input.setHint("ENTER LETTERS");
        input.setHintTextColor(Color.LTGRAY);
        input.setTextColor(Color.WHITE);
        layout.addView(input);

        Button solveBtn = new Button(this);
        solveBtn.setText("SOLVE");
        solveBtn.setBackgroundColor(Color.parseColor("#E91E63")); // Pink
        solveBtn.setTextColor(Color.WHITE);
        layout.addView(solveBtn);

        TextView resultsView = new TextView(this);
        resultsView.setTextColor(Color.WHITE);
        resultsView.setMaxLines(10); // Limit height
        layout.addView(resultsView);

        solveBtn.setOnClickListener(v -> {
            if (!isLoaded) {
                Toast.makeText(this, "Still loading dictionary...", Toast.LENGTH_SHORT).show();
                return;
            }
            String letters = input.getText().toString();
            List<String> found = trie.solve(letters);
            resultsView.setText(found.isEmpty() ? "No words found" : found.toString());
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT);
        
        params.gravity = Gravity.TOP;
        params.y = 150;

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
