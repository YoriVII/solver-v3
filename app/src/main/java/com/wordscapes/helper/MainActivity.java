package com.wordscapes.helper;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        Button enableBtn = new Button(this);
        enableBtn.setText("ENABLE OVERLAY");
        enableBtn.setOnClickListener(v -> {
            startService(new Intent(this, OverlayService.class));
        });
        layout.addView(enableBtn);
        setContentView(layout);
    }
}
