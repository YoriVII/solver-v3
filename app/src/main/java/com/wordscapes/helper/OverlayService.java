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

    private static class Tile {
        char letter;
        float x, y;
        boolean used;
        Tile(char letter, float x, float y) {
            this.letter = letter;
            this.x = x;
            this.y = y;
            this.used = false;
        }
    }

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

        btnPlay = createPanelButton("▶", Color.GREEN);
        btnPlay.setOnClickListener(v -> {
            if (isSolving) stopSolving();
            else startSolving();
        });
        controlPanel.addView(btnPlay);

        TextView btnAdd = createPanelButton("➕", Color.WHITE);
        btnAdd.setOnClickListener(v -> addCircle(circleViews.size()));
        controlPanel.addView(btnAdd);

        TextView btnSub = createPanelButton("➖", Color.WHITE);
        btnSub.setOnClickListener(v -> removeLastCircle());
        controlPanel.addView(btnSub);

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
        circle.setBackgroundResource(R.drawable.circle_shape);
        circle.setTextColor(Color.WHITE);
        circle.setGravity(Gravity.CENTER);
        circle.setTextSize(20);
        circle.setFilters(new android.text.InputFilter[] { new android.text.InputFilter.LengthFilter(1) });
        circle.setSingleLine(true);
        circle.setCursorVisible(false);
        circle.setOnClickListener(v -> circle.setCursorVisible(true));
        circle.setOnFocusChangeListener((v, hasFocus) -> circle.setCursorVisible(hasFocus));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            CIRCLE_SIZE, CIRCLE_SIZE,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, 
            PixelFormat.TRANSLUCENT);

        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 300 + (index % 3 * 150); 
        lp.y = 500 + (index / 3 * 150);

        circle.setOnTouchListener(new View.OnTouchListener() {
            int lastX, lastY, initialX, initialY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
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

    private void setWindowsTouchable(boolean touchable) {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (EditText circle : circleViews) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) circle.getLayoutParams();
                if (touchable) {
                    params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
                    circle.setAlpha(1.0f);
                } else {
                    params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                    circle.setAlpha(0.7f);
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
        SwiperService.instance.reset();

        for (EditText c : circleViews) {
            c.clearFocus();
            c.setCursorVisible(false);
        }
        setWindowsTouchable(false);

        List<Tile> boardTiles = new ArrayList<>();
        StringBuilder inputLetters = new StringBuilder();
        
        for (EditText circle : circleViews) {
            String txt = circle.getText().toString().toUpperCase();
            if (!txt.isEmpty()) {
                char c = txt.charAt(0);
                inputLetters.append(c);
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) circle.getLayoutParams();
                
                // SUPER ACCURATE CENTER CALCULATION
                // We add exactly half the circle size to X and Y
                float centerX = params.x + (CIRCLE_SIZE / 2.0f);
                float centerY = params.y + (CIRCLE_SIZE / 2.0f);
                boardTiles.add(new Tile(c, centerX, centerY));
            }
        }

        if (inputLetters.length() < 3) {
            Toast.makeText(this, "Need 3+ letters", Toast.LENGTH_SHORT).show();
            stopSolving();
            return;
        }

        new Thread(() -> {
            List<String> rawWords = trie.solve(inputLetters.toString());
            List<String> validWords = new ArrayList<>();
            for (String word : rawWords) {
                if (canMakeWord(word, boardTiles)) validWords.add(word);
            }
            
            new Handler(Looper.getMainLooper()).post(() -> 
                Toast.makeText(this, "Solving " + validWords.size() + " words", Toast.LENGTH_SHORT).show()
            );

            for (String word : validWords) {
                if (!isSolving) break;
                float[][] path = buildPathForWord(word, boardTiles);
                if (path != null) {
                    SwiperService.instance.swipe(path);
                    try { Thread.sleep(600); } catch (InterruptedException e) {}
                }
            }
            stopSolving();
        }).start();
    }

    private boolean canMakeWord(String word, List<Tile> board) {
        return buildPathForWord(word, board) != null;
    }

    private float[][] buildPathForWord(String word, List<Tile> board) {
        float[][] path = new float[word.length()][2];
        for (Tile t : board) t.used = false;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            Tile found = null;
            // Find closest unused tile (Euclidean distance could be added here for even more precision if needed)
            for (Tile t : board) {
                if (t.letter == c && !t.used) {
                    found = t;
                    break;
                }
            }
            if (found == null) return null;
            found.used = true;
            path[i][0] = found.x;
            path[i][1] = found.y;
        }
        return path;
    }

    private void stopSolving() {
        isSolving = false;
        if (SwiperService.instance != null) SwiperService.instance.stopSwiping();
        updateUIState(false);
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
