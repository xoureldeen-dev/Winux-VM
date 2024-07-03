package com.vectras.boxvidra.activities;

import static android.os.Build.VERSION.SDK_INT;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.system.ErrnoException;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.termux.app.TermuxActivity;
import com.vectras.boxvidra.R;
import com.vectras.boxvidra.core.TermuxX11;
import com.vectras.boxvidra.services.MainService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static Activity activity;
    MaterialButton startX11Btn, startXfce4Btn, openTerminalBtn;

    public static TextView termuxX11TextView, tvLogger;

    boolean isX11Started = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;

        startX11Btn = findViewById(R.id.startX11);
        startX11Btn.setOnClickListener(this);

        startXfce4Btn = findViewById(R.id.startXfce4);
        startXfce4Btn.setOnClickListener(this);

        openTerminalBtn = findViewById(R.id.openTerminal);
        openTerminalBtn.setOnClickListener(this);

        termuxX11TextView = findViewById(R.id.tvIsTermuxX11);
        tvLogger = findViewById(R.id.tvLogger);
        tvLogger.setTypeface(Typeface.MONOSPACE);

        if (isX11Started) {
            termuxX11TextView.setText(R.string.termuxx11_service_yes);
        } else {
            termuxX11TextView.setText(R.string.termuxx11_service_no);
        }

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.startX11) {
            try {
                isX11Started = true;
                startX11Btn.setEnabled(false);
                termuxX11TextView.setText(R.string.termuxx11_service_yes);
                TermuxX11.main(new String[]{":0"});
            } catch (ErrnoException e) {
                isX11Started = false;
                startX11Btn.setEnabled(true);
                termuxX11TextView.setText(R.string.termuxx11_service_no);
            }
        } else if (id == R.id.startXfce4) {
            Intent serviceIntent = new Intent(activity, MainService.class);
            if (SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(serviceIntent);
            } else {
                activity.startService(serviceIntent);
            }
        } else if (id == R.id.openTerminal) {
            startActivity(new Intent(activity, TermuxActivity.class));
        }
    }
}