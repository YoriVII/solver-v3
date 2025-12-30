package com.wordscapes.helper;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.*;

public class OverlayService extends Service {
    private WindowManager wm;
    private FrameLayout circlesContainer; // Holds the circles
    private LinearLayout controlPanel;    // The sidebar
    private Trie trie;
    private final List<EditText> circles = new ArrayList<>();
    private boolean isSolving = false;
    
    // Settings
    private static final int CIRCLE_SIZE = 130;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        trie = new Trie();
        new Thread(() -> trie.loadDictionary(this)).start();

        // 1. Layer for Circles (Fullscreen, transparent)
        WindowManager.LayoutParams containerParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // Passes touches through empty space
            PixelFormat.TRANSLUCENT);
        
        circlesContainer = new FrameLayout(this);
        wm.addView(circlesContainer, containerParams);

        // 2. Layer for Control Panel (Small, floating sidebar)
        createControlPanel();

        // 3. Start with 6 circles by default
        for(int i=0; i<6; i++) addCircle(i);
    }

    private void createControlPanel() {
        controlPanel = new LinearLayout(this);
        controlPanel.setOrientation(LinearLayout.VERTICAL);
        controlPanel.setBackgroundColor(Color.parseColor("#EE222222")); // Dark BG
        controlPanel.setPadding(10, 20, 10, 20);

        // -- PLAY BUTTON --
        TextView btnPlay = createPanelButton("▶", Color.GREEN);
        btnPlay.setOnClickListener(v -> {
            if (isSolving) {
                stopSolving();
                btnPlay.setText("▶");
                btnPlay.setTextColor(Color.GREEN);
            } else {
                startSolving();
                btnPlay.setText("■"); // Square for stop
                btnPlay.setTextColor(Color.RED);
            }
        });
        controlPanel.addView(btnPlay);

        // -- ADD BUTTON --
        TextView btnAdd = createPanelButton("➕", Color.WHITE);
        btnAdd.setOnClickListener(v -> addCircle(circles.size()));
        controlPanel.addView(btnAdd);

        // -- REMOVE BUTTON --
        TextView btnSub = createPanelButton("➖", Color.WHITE);
        btnSub.setOnClickListener(v -> removeLastCircle());
        controlPanel.addView(btnSub);

        // -- MOVE HANDLE --
        TextView btnMove = createPanelButton("✥", Color.CYAN);
        // Logic to drag the panel itself
        btnMove.setOnTouchListener(new View.OnTouchListener() {
            int lastX, lastY, initialX, initialY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WindowManager.LayoutParams p = (WindowManager.LayoutParams) controlPanel.getLayoutParams();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        initialX = p.x;
                        initialY = p.y;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        p.x = initialX + ((int) event.getRawX() - lastX);
                        p.y = initialY + ((int) event.getRawY() - lastY);
                        wm.updateViewLayout(controlPanel, p);
                        return true;
                }
                return false;
            }
        });
        controlPanel.addView(btnMove);

        // Add Panel to Window
        WindowManager.LayoutParams panelParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = 0;
        panelParams.y = 300;
        
        wm.addView(controlPanel, panelParams);
    }

    private TextView createPanelButton(String text, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(26);
        tv.setTextColor(color);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(10, 20, 10, 20);
        return tv;
    }

    private void addCircle(int index) {
        EditText circle = new EditText(this);
        circle.setBackgroundResource(android.R.drawable.btn_default_small);
        circle.setBackgroundColor(Color.parseColor("#4000BCD4")); // Transparent Cyan
        circle.setTextColor(Color.WHITE);
        circle.setGravity(Gravity.CENTER);
        circle.setTextSize(20);
        circle.setFilters(new android.text.InputFilter[] { new android.text.InputFilter.LengthFilter(1) });
        
        // Initial Staggered Position
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(CIRCLE_SIZE, CIRCLE_SIZE);
        lp.leftMargin = 300 + (index % 3 * 150); 
        lp.topMargin = 500 + (index / 3 * 150);
        circle.setLayoutParams(lp);

        // Drag Listener for Circle
        circle.setOnTouchListener(new View.OnTouchListener() {
            int lastX, lastY, initialX, initialY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        initialX = ((FrameLayout.LayoutParams) v.getLayoutParams()).leftMargin;
                        initialY = ((FrameLayout.LayoutParams) v.getLayoutParams()).topMargin;
                        return false; 
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) event.getRawX() - lastX;
                        int dy = (int) event.getRawY() - lastY;
                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();
                        params.leftMargin = initialX + dx;
                        params.topMargin = initialY + dy;
                        v.setLayoutParams(params);
                        return true;
                }
                return false;
            }
        });

        circles.add(circle);
        circlesContainer.addView(circle);
    }

    private void removeLastCircle() {
        if (!circles.isEmpty()) {
            View v = circles.remove(circles.size() - 1);
            circlesContainer.removeView(v);
        }
    }

    private void startSolving() {
        isSolving = true;
        if (SwiperService.instance != null) SwiperService.instance.reset();

        // Map Letters
        Map<Character, PointF> letterMap = new HashMap<>();
        StringBuilder inputLetters = new StringBuilder();
        
        for (EditText circle : circles) {
            String txt = circle.getText().toString().toUpperCase();
            if (!txt.isEmpty()) {
                char c = txt.charAt(0);
                inputLetters.append(c);
                int[] location = new int[2];
                circle.getLocationOnScreen(location);
                letterMap.put(c, new PointF(location[0] + (CIRCLE_SIZE/2f), location[1] + (CIRCLE_SIZE/2f)));
            }
        }

        if (inputLetters.length() < 3) {
            Toast.makeText(this, "Need at least 3 letters", Toast.LENGTH_SHORT).show();
            isSolving = false;
            return;
        }

        // Solve and Swipe
        new Thread(() -> {
            List<String> words = trie.solve(inputLetters.toString());
            
            for (String word : words) {
                if (!isSolving) break; // User pressed stop
                if (SwiperService.instance == null) break;

                float[][] path = new float[word.length()][2];
                for (int i = 0; i < word.length(); i++) {
                    PointF p = letterMap.get(word.charAt(i));
                    path[i][0] = p.x;
                    path[i][1] = p.y;
                }

                SwiperService.instance.swipe(path);
                try { Thread.sleep(600); } catch (InterruptedException e) {}
            }
            
            // Finished
            isSolving = false;
            new Handler(Looper.getMainLooper()).post(() -> {
               // Update UI if needed
            });
        }).start();
    }

    private void stopSolving() {
        isSolving = false;
        if (SwiperService.instance != null) SwiperService.instance.stopSwiping();
        Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (circlesContainer != null) wm.removeView(circlesContainer);
        if (controlPanel != null) wm.removeView(controlPanel);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
