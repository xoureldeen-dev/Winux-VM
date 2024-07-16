package com.vectras.boxvidra.activities;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vectras.boxvidra.R;
import com.vectras.boxvidra.core.ObbParser;
import com.vectras.boxvidra.fragments.OptionsFragment;
import com.vectras.boxvidra.fragments.WinePrefixFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public static Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity = this;

        clearNotifications();

        ImageView iconImageView = findViewById(R.id.imageView);

        try {
            JSONObject jsonObject = ObbParser.obbParse(this);

            String iconName = jsonObject.getString("iconName");
            String filesDir = getFilesDir().getAbsolutePath();
            String iconPath = filesDir + "/" + iconName;

            File imgFile = new File(iconPath);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                iconImageView.setImageBitmap(myBitmap);
            } else {
                iconImageView.setImageResource(R.mipmap.ic_launcher);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new WinePrefixFragment())
                    .commit();
        }

        // Setup bottom navigation view
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            /*if (item.getItemId() == R.id.menu_home) {
                selectedFragment = new HomeFragment();
            } else*/ if (item.getItemId() == R.id.menu_wine_prefixes) {
                selectedFragment = new WinePrefixFragment();
            } else if (item.getItemId() == R.id.menu_options) {
                selectedFragment = new OptionsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        getWindow().setNavigationBarColor(getResources().getColor(R.color.darkPurple));

        TextView tvAppInfo = findViewById(R.id.tvAppInfo);

        // Update App Info
        try {
            JSONObject jsonObject = ObbParser.obbParse(activity);
            String info = ObbParser.parseJsonAndFormat(jsonObject);
            tvAppInfo.setText(info);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            tvAppInfo.setText(R.string.failed_to_load_data);
        }

        ImageButton discordButton = findViewById(R.id.discordButton);
        ImageButton telegramButton = findViewById(R.id.telegramButton);
        ImageButton websiteButton = findViewById(R.id.websiteButton);

        discordButton.setOnClickListener(v -> {
            openUrlInBrowser("https://discord.gg/4GNkya3D");
        });

        telegramButton.setOnClickListener(v -> {
            openUrlInBrowser("https://t.me/vectras_os");
        });

        websiteButton.setOnClickListener(v -> openUrlInBrowser("https://boxvidra.blackstorm.cc/"));
    }

    private void openUrlInBrowser(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    public static void clearNotifications() {
        NotificationManager notificationManager = (NotificationManager) activity.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

}