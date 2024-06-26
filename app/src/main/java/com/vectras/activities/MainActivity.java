package com.vectras.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.vectras.term.R;

public class MainActivity extends AppCompatActivity {

    public static Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextInputEditText startupCommandEditText = findViewById(R.id.startupCommandEditText);

        Button btnLaunch = findViewById(R.id.btnLaunch);
        btnLaunch.setOnClickListener(v -> startActivity(new Intent(activity, x.org.server.MainActivity.class).putExtra("STARTUP_CMD", startupCommandEditText.getText().toString())));
    }

}