package com.vectras.boxvidra.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.vectras.boxvidra.R;
import com.vectras.boxvidra.utils.FileUtils;
import com.vectras.boxvidra.view.ZoomableTextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class SplashActivity extends AppCompatActivity {
    private static final long REQUIRED_FREE_SPACE = 7L * 1024 * 1024 * 1024; // 7GB in bytes
    private static final int REQUEST_PICK_OBB_FILE = 1;
    private static final int REQUEST_PERMISSIONS = 2;

    private Activity activity;
    private ZoomableTextView vterm;
    private ProgressBar progressBar;
    private final int obbVersion = 1;
    private final String obbName = "main." + obbVersion + ".com.vectras.boxvidra.obb";
    private boolean obbPickedManually = false; // Flag to track if OBB file was picked manually

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        activity = this;

        if (checkDataFiles()) {
            launchMainActivity();
            return;
        }

        progressBar = findViewById(R.id.progressBar);
        vterm = findViewById(R.id.tvTerminalOutput);

        if (!checkSupportedGPU()) {
            showUnsupportedGpuDialog();
        } else {
            showAlphaStageDialog();
        }
    }

    private boolean checkDataFiles() {
        File usrDir = new File(getFilesDir(), "usr");
        File homeDir = new File(getFilesDir(), "home");
        return usrDir.exists() && usrDir.isDirectory() && homeDir.exists() && homeDir.isDirectory();
    }

    private void launchMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private boolean checkSupportedGPU() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        String glRenderer = configurationInfo.getGlEsVersion();
        return glRenderer != null && glRenderer.startsWith("3.2");
    }

    private void showUnsupportedGpuDialog() {
        new AlertDialog.Builder(this, R.style.MainDialogTheme)
                .setTitle("Unsupported GPU")
                .setMessage("This app is currently in alpha stage and supports only devices with Adreno GPU. Some features may be unstable or missing.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showAlphaStageDialog() {
        String discordLink = "https://discord.gg/wK3cYvhE";
        String telegramLink = "https://t.me/vectras_os";
        String githubIssuesLink = "https://github.com/Boxvidra/Boxvidra-Android/issues";

        SpannableString spannableMessage = new SpannableString(
                "This app is currently in alpha stage and under active development. " +
                        "It supports only devices with Adreno GPU. Some features may be unstable or missing. " +
                        "Your feedback is valuable for improving the app. " +
                        "Join our community for support and to report issues:\n\n" +
                        "Discord: " + discordLink + "\n" +
                        "Telegram: " + telegramLink + "\n" +
                        "GitHub Issues: " + githubIssuesLink
        );

        ClickableSpan discordSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                openUrlInBrowser(discordLink);
            }
        };
        ClickableSpan telegramSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                openUrlInBrowser(telegramLink);
            }
        };
        ClickableSpan githubIssuesSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                openUrlInBrowser(githubIssuesLink);
            }
        };

        spannableMessage.setSpan(discordSpan, spannableMessage.toString().indexOf(discordLink),
                spannableMessage.toString().indexOf(discordLink) + discordLink.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableMessage.setSpan(telegramSpan, spannableMessage.toString().indexOf(telegramLink),
                spannableMessage.toString().indexOf(telegramLink) + telegramLink.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableMessage.setSpan(githubIssuesSpan, spannableMessage.toString().indexOf(githubIssuesLink),
                spannableMessage.toString().indexOf(githubIssuesLink) + githubIssuesLink.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.MainDialogTheme)
                .setTitle("Alpha Stage Notice")
                .setMessage(spannableMessage)
                .setPositiveButton("OK", (dialog1, which) -> {
                    if (checkAndRequestPermissions()) {
                        initializeApp();
                    }
                })
                .setCancelable(false)
                .create();

        dialog.show();

        TextView messageTextView = dialog.findViewById(android.R.id.message);
        if (messageTextView != null) {
            messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private void openUrlInBrowser(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private boolean checkAndRequestPermissions() {
        int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        String[] permissionsToRequest = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (readPermission != PackageManager.PERMISSION_GRANTED || writePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void initializeApp() {
        if (!checkFreeSpace()) {
            showLowSpaceDialog();
            return;
        }

        File obbFile = new File(activity.getObbDir() + "/" + obbName);
        if (!obbFile.exists()) {
            showObbNotFoundDialog();
        } else {
            if (!obbPickedManually) {
                setupVectras(obbFile);
            } else {
                setupWithObbFile(Uri.fromFile(obbFile));
            }
        }
    }

    private boolean checkFreeSpace() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long bytesAvailable = (long) stat.getBlockSizeLong() * (long) stat.getAvailableBlocksLong();
        return bytesAvailable >= REQUIRED_FREE_SPACE;
    }

    private void showLowSpaceDialog() {
        new AlertDialog.Builder(activity, R.style.MainDialogTheme)
                .setTitle("Low Space Warning")
                .setMessage("You do not have enough free space to continue.")
                .setPositiveButton("Exit", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showObbNotFoundDialog() {
        new AlertDialog.Builder(activity, R.style.MainDialogTheme)
                .setTitle("OBB file not found")
                .setMessage("Please select the OBB file manually to continue.")
                .setPositiveButton("Pick OBB", (dialog, which) -> pickObbFile())
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void pickObbFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Specify the MIME type of the files you want to pick here
        startActivityForResult(intent, REQUEST_PICK_OBB_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_OBB_FILE && resultCode == RESULT_OK && data != null) {
            Uri obbFileUri = data.getData();
            if (obbFileUri != null) {
                // Check if the selected file meets the criteria
                String obbFilePath = FileUtils.getPath(activity, obbFileUri);
                if (obbFilePath != null) {
                    File obbFile = new File(obbFilePath);
                    if (obbFile.getName().toLowerCase().endsWith(".obb") && obbFile.length() > 700 * 1024 * 1024) {
                        setupWithObbFile(obbFileUri);
                        obbPickedManually = true; // Set the flag to true when OBB is picked manually
                    } else {
                        showInvalidObbFileDialog();
                    }
                } else {
                    finish();
                }
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    private void showInvalidObbFileDialog() {
        // Create a SpannableString to make the URL clickable
        SpannableString message = new SpannableString("Please select an OBB file with the extension '.obb'.\n\nFor the correct OBB file, please download from the official website: boxvidra.blackstorm.cc");

        // Define the clickable span for the URL
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // Open the URL in a browser or perform any desired action
                Uri uri = Uri.parse("https://boxvidra.blackstorm.cc");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        };

        // Set the clickable span for the URL
        message.setSpan(clickableSpan, message.toString().indexOf("boxvidra.blackstorm.cc"), message.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Create the AlertDialog with a TextView to display the message
        TextView textView = new TextView(activity);
        textView.setText(message);
        textView.setMovementMethod(LinkMovementMethod.getInstance()); // Make links clickable
        textView.setPadding(40, 20, 40, 20); // Add padding for better appearance

        new AlertDialog.Builder(activity, R.style.MainDialogTheme)
                .setTitle("Invalid OBB File")
                .setView(textView)
                .setPositiveButton("Re-pick OBB", (dialog, which) -> pickObbFile())
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void setupWithObbFile(Uri obbFileUri) {
        // Resolve the URI to get the actual file path
        String obbFilePath = FileUtils.getPath(activity, obbFileUri);
        if (obbFilePath != null) {
            File obbFile = new File(obbFilePath);
            setupVectras(obbFile); // Call setupVectras with the manually picked OBB file
        } else {
            Toast.makeText(activity, "Failed to get the file path.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupVectras(File obbFile) {
        progressBar.setVisibility(View.VISIBLE);
        String filesDir = activity.getFilesDir().getAbsolutePath();

        File distroDir = new File(filesDir);
        executeShellCommand("set -e;" +
                " echo 'Starting setup...';" +
                " echo 'Extracting " + obbFile.getAbsolutePath() + "...';" +
                " tar -xzf " + obbFile.getAbsolutePath() + " -C " + distroDir.getAbsolutePath() + ";" +
                " echo 'Extract Successful!';" +
                // Do not delete the OBB file when picked manually
                // (obbPickedManually ? "" : " echo 'Removing " + obbFile.getAbsolutePath() + "...'; rm " + obbFile.getAbsolutePath() + ";") +
                " echo \"installation successful! xssFjnj58Id\"");
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
                    });
                }
            } catch (IOException | InterruptedException e) {
                // Handle exceptions by printing the stack trace in the terminal output
                final String errorMessage = e.getMessage();
                activity.runOnUiThread(() -> {
                    appendTextAndScroll("Error: " + errorMessage + "\n");
                    Toast.makeText(activity, "Error executing command: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        }).start(); // Execute the command in a separate thread to prevent blocking the UI thread
    }

    // Handle permission request results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                // Check if all permissions were granted
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initializeApp(); // Initialize the app after permissions are granted
                } else {
                    // Permission denied, show explanation or request again
                    if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        showPermissionRationaleDialog();
                    } else {
                        showPermissionSettingsDialog();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // Show rationale dialog explaining why permissions are needed
    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this, R.style.MainDialogTheme)
                .setTitle("Permissions Required")
                .setMessage("To continue, the app needs storage access permissions.")
                .setPositiveButton("OK", (dialog, which) -> checkAndRequestPermissions())
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    // Show dialog directing user to app settings to enable permissions
    private void showPermissionSettingsDialog() {
        new AlertDialog.Builder(this, R.style.MainDialogTheme)
                .setTitle("Permissions Required")
                .setMessage("Permissions were denied. Please enable them in App Settings to continue.")
                .setPositiveButton("App Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
}
