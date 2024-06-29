package com.vectras.boxvidra.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.system.ErrnoException;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.vectras.boxvidra.R;
import com.vectras.boxvidra.core.TermuxX11;
import com.vectras.boxvidra.utils.FileUtils;
import com.vectras.vterm.Terminal;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public static Activity activity;
    public Terminal terminal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;

        terminal = new Terminal(activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextInputEditText startupCommandEditText = findViewById(R.id.startupCommandEditText);

        Button btnLaunch = findViewById(R.id.btnLaunch);
        btnLaunch.setOnClickListener(v ->
        {
            try {
                TermuxX11.main(new String[]{":0"});
            } catch (ErrnoException e) {
                throw new RuntimeException(e);
            }

            terminal.executeShellCommand(startupCommandEditText.getText().toString());
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.termux.x11", "com.termux.x11.MainActivity"));

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Target activity not found.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to start the x11 activity.", Toast.LENGTH_SHORT).show();
            }

        });
        Button btnVterm = findViewById(R.id.btnVterm);
        btnVterm.setOnClickListener(v ->
        {
            terminal.showVterm();
        });

        FileUtils.copyFolderFromAssets(getApplicationContext(), "X11", getApplicationContext().getFilesDir() + "/X11");
    }

}