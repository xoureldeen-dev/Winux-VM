package com.vectras.boxvidra.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.vectras.boxvidra.R;
import com.vectras.boxvidra.utils.FileUtils;
import com.vectras.boxvidra.view.ZoomableTextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class SplashActivity extends AppCompatActivity implements View.OnClickListener {
    Activity activity;
    ZoomableTextView vterm;
    Button inBtn;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        activity = this;

        progressBar = findViewById(R.id.progressBar);

        vterm = findViewById(R.id.tvTerminalOutput);

        inBtn = findViewById(R.id.btnInstall);
        inBtn.setOnClickListener(this);

        File obbFile = new File(activity.getObbDir() + "/bootstrap-2447-arm64-v8a.tar.gz");

        String filesDir = activity.getFilesDir().getAbsolutePath();
        File distroDir = new File(filesDir + "/usr");
        if (!distroDir.exists()) {
            distroDir.mkdirs();
        }
        File[] distDirList = distroDir.listFiles();
        if (distDirList.length != 0) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            if (!obbFile.exists()) {
                new AlertDialog.Builder(activity)
                        .setTitle("Obb file not found!")
                        .setCancelable(false)
                        .setMessage("please download and copy bootstrap-2447-arm64-v8a.tar.gz to 'Android/obb' dir")
                        .show();
            }
        }
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnInstall) {
            setupVectras();
        }
    }

    // Function to append text and automatically scroll to bottom
    private void appendTextAndScroll(String textToAdd) {
        ScrollView scrollView = findViewById(R.id.scrollView);

        // Update the text
        vterm.append(textToAdd);

        if (textToAdd.contains("xssFjnj58Id")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        // Scroll to the bottom
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    public void onBackPressed() {
        super.onBackPressed();
    }

    // Method to execute the shell command
    public void executeShellCommand(String userCommand) {
        new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();

                processBuilder.command("/system/bin/sh");
                Process process = processBuilder.start();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                writer.write(userCommand);
                writer.newLine();
                writer.flush();
                writer.close();

                String line;
                while ((line = reader.readLine()) != null) {
                    final String outputLine = line;
                    activity.runOnUiThread(() -> appendTextAndScroll(outputLine + "\n"));
                }

                // Read any errors from the error stream
                while ((line = errorReader.readLine()) != null) {
                    final String errorLine = line;
                    activity.runOnUiThread(() -> appendTextAndScroll(errorLine + "\n"));
                }

                // Clean up
                reader.close();
                errorReader.close();

                // Wait for the process to finish
                process.waitFor();

                // Wait for the process to finish
                int exitValue = process.waitFor();

                // Check if the exit value indicates an error
                if (exitValue != 0) {
                    // If exit value is not zero, display a toast message
                    String toastMessage = "Command failed with exit code: " + exitValue;
                    activity.runOnUiThread(() -> {
                        appendTextAndScroll("Error: " + toastMessage + "\n");
                        Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show();
                        inBtn.setVisibility(View.VISIBLE);
                    });
                }
            } catch (IOException | InterruptedException e) {
                // Handle exceptions by printing the stack trace in the terminal output
                final String errorMessage = e.getMessage();
                activity.runOnUiThread(() -> {
                    appendTextAndScroll("Error: " + errorMessage + "\n");
                    Toast.makeText(activity, "Error executing command: " + errorMessage, Toast.LENGTH_LONG).show();
                    inBtn.setVisibility(View.VISIBLE);
                });
            }
        }).start(); // Execute the command in a separate thread to prevent blocking the UI thread
    }

    private void setupVectras() {
        inBtn.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        String filesDir = activity.getFilesDir().getAbsolutePath();

        File distroDir = new File(filesDir + "/usr");
        File obbFile = new File(activity.getObbDir() + "/bootstrap-2447-arm64-v8a.tar.gz");
        executeShellCommand("set -e;" +
                " echo 'Starting setup...';" +
                " tar -xzf " + obbFile.getAbsolutePath() + " -C " + distroDir.getAbsolutePath() + "/../;" +
                " echo \"installation successful! xssFjnj58Id\"");
        FileUtils.copyFolderFromAssets(getApplicationContext(), "X11", getApplicationContext().getFilesDir() + "/X11");
    }

}