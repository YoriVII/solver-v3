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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.*;

public class OverlayService extends Service {
    private WindowManager wm;
    private LinearLayout controlPanel;
    private Trie trie;
    private final List<EditText> circleViews = new ArrayList<>();
    private TextView btnPlay;
    private boolean isSolving = false;
    
    private static final int CIRCLE_SIZE = 130;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        trie = new Trie();
        new Thread(() -> trie.loadDictionary(this)).start();

        createControlPanel();
        for(int i=0; i<6; i++) addCircle(i);
    }

    private void createControlPanel() {
        controlPanel = new LinearLayout(this);
        controlPanel.setOrientation(LinearLayout.VERTICAL);
        controlPanel.setBackgroundColor(Color.parseColor("#EE222222"));
        controlPanel.setPadding(10, 20, 10, 20);

        // -- PLAY BUTTON --
        btnPlay = createPanelButton("▶", Color.GREEN);
        btnPlay.setOnClickListener(v -> {
            if (isSolving) {
                stopSolving();
            } else {
                startSolving();
            }
        });
        controlPanel.addView(btnPlay);

        // -- ADD BUTTON --
        TextView btnAdd = createPanelButton("➕", Color.WHITE);
        btnAdd.setOnClickListener(v -> addCircle(circleViews.size()));
        controlPanel.addView(btnAdd);

        // -- REMOVE BUTTON --
        TextView btnSub = createPanelButton("➖", Color.WHITE);
        btnSub.setOnClickListener(v -> removeLastCircle());
        controlPanel.addView(btnSub);

        // -- MOVE HANDLE --
        TextView btnMove = createPanelButton("✥", Color.CYAN);
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
        circle.setBackgroundColor(Color.parseColor("#4000BCD4"));
        circle.setTextColor(Color.WHITE);
        circle.setGravity(Gravity.CENTER);
        circle.setTextSize(20);
        circle.setFilters(new android.text.InputFilter[] { new android.text.InputFilter.LengthFilter(1) });
        circle.setSingleLine(true);
        circle.setCursorVisible(false);
        
        circle.setOnClickListener(v -> circle.setCursorVisible(true));
        circle.setOnFocusChangeListener((v, hasFocus) -> circle.setCursorVisible(hasFocus));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            CIRCLE_SIZE, 
            CIRCLE_SIZE,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, 
            PixelFormat.TRANSLUCENT);

        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 300 + (index % 3 * 150); 
        lp.y = 500 + (index / 3 * 150);

        // Drag Listener
        circle.setOnTouchListener(new View.OnTouchListener() {
            int lastX, lastY, initialX, initialY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // IMPORTANT: If solving, IGNORE touches so we don't drag while swiping
                if (isSolving) return false;

                WindowManager.LayoutParams params = (WindowManager.LayoutParams) v.getLayoutParams();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        initialX = params.x;
                        initialY = params.y;
                        return false; 
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + ((int) event.getRawX() - lastX);
                        params.y = initialY + ((int) event.getRawY() - lastY);
                        wm.updateViewLayout(v, params);
                        return true;
                }
                return false;
            }
        });

        circleViews.add(circle);
        wm.addView(circle, lp);
    }

    private void removeLastCircle() {
        if (!circleViews.isEmpty()) {
            EditText v = circleViews.remove(circleViews.size() - 1);
            wm.removeView(v);
        }
    }

    // NEW: Helper to lock/unlock windows to allow swipes to pass through
    private void setWindowsTouchable(boolean touchable) {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (EditText circle : circleViews) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) circle.getLayoutParams();
                if (touchable) {
                    // Make interactive again
                    params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
                } else {
                    // Make "Ghost" (Untouchable) so swipes hit the game below
                    params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                }
                wm.updateViewLayout(circle, params);
            }
        });
    }

    private void updateUIState(boolean solving) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (solving) {
                btnPlay.setText("■");
                btnPlay.setTextColor(Color.RED);
            } else {
                btnPlay.setText("▶");
                btnPlay.setTextColor(Color.GREEN);
            }
        });
    }

    private void startSolving() {
        if (SwiperService.instance == null) {
            Toast.makeText(this, "Swipe Service NOT Ready!", Toast.LENGTH_LONG).show();
            return;
        }

        isSolving = true;
        updateUIState(true);
        if (SwiperService.instance != null) SwiperService.instance.reset();

        // 1. Clear focus
        for (EditText c : circleViews) {
            c.clearFocus();
            c.setCursorVisible(false);
        }

        // 2. LOCK WINDOWS (Fixes the dragging issue)
        setWindowsTouchable(false);

        Map<Character, PointF> letterMap = new HashMap<>();
        StringBuilder inputLetters = new StringBuilder();
        
        for (EditText circle : circleViews) {
            String txt = circle.getText().toString().toUpperCase();
            if (!txt.isEmpty()) {
                char c = txt.charAt(0);
                inputLetters.append(c);
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) circle.getLayoutParams();
                float centerX = params.x + (CIRCLE_SIZE / 2f);
                float centerY = params.y + (CIRCLE_SIZE / 2f);
                letterMap.put(c, new PointF(centerX, centerY));
            }
        }

        if (inputLetters.length() < 3) {
            Toast.makeText(this, "Need 3+ letters", Toast.LENGTH_SHORT).show();
            stopSolving(); // Make sure to unlock if failing
            return;
        }

        new Thread(() -> {
            List<String> words = trie.solve(inputLetters.toString());
            
            new Handler(Looper.getMainLooper()).post(() -> 
                Toast.makeText(this, "Found " + words.size() + " words.", Toast.LENGTH_SHORT).show()
            );
            
            for (String word : words) {
                if (!isSolving) break;
                if (SwiperService.instance == null) break;

                float[][] path = new float[word.length()][2];
                for (int i = 0; i < word.length(); i++) {
                    PointF p = letterMap.get(word.charAt(i));
                    if (p != null) {
                        path[i][0] = p.x;
                        path[i][1] = p.y;
                    }
                }

                SwiperService.instance.swipe(path);
                try { Thread.sleep(600); } catch (InterruptedException e) {}
            }
            
            stopSolving();
        }).start();
    }

    private void stopSolving() {
        isSolving = false;
        if (SwiperService.instance != null) SwiperService.instance.stopSwiping();
        updateUIState(false);
        // UNLOCK WINDOWS (Make interactive again)
        setWindowsTouchable(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (View v : circleViews) wm.removeView(v);
        if (controlPanel != null) wm.removeView(controlPanel);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
