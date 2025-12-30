package com.wordscapes.helper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Setting up a simple dark layout programmatically to match your screenshot
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xFF212121);
        layout.setGravity(android.view.Gravity.CENTER);

        android.widget.TextView title = new android.widget.TextView(this);
        title.setText("Wordscapes Helper");
        title.setTextSize(24);
        title.setTextColor(0xFFFFFFFF);
        title.setPadding(0, 0, 0, 50);
        layout.addView(title);

        Button enableBtn = new Button(this);
        enableBtn.setText("ENABLE");
        enableBtn.setBackgroundColor(0xFFE91E63); // Pink color from your screenshot
        enableBtn.setTextColor(0xFFFFFFFF);
        
        enableBtn.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                startService(new Intent(this, OverlayService.class));
            }
        });

        layout.addView(enableBtn);
        setContentView(layout);
    }
}
