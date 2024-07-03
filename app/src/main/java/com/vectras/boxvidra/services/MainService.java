package com.vectras.boxvidra.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.core.app.NotificationCompat;

import com.termux.app.TermuxService;
import com.vectras.boxvidra.R;
import com.vectras.boxvidra.activities.MainActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class MainService extends Service {
    public static String CHANNEL_ID = "Boxvidra Service";
    private static final int NOTIFICATION_ID = 1;
    private String TAG = "MainService";
    public static MainService service;

    @Override
    public void onCreate() {
        super.onCreate();
        service = this;
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Boxvidra")
                .setContentText("Boxvidra running in background.")
                .setSmallIcon(R.drawable.ic_main_service_icon)
                .build();

        executeShellCommand(TermuxService.PREFIX_PATH + "/bin/proot-distro login --isolated ubuntu --shared-tmp -- /bin/bash -c  'export PULSE_SERVER=127.0.0.1 && export XDG_RUNTIME_DIR=${TMPDIR} && env DISPLAY=:0 xfce4-session'");

        startForeground(NOTIFICATION_ID, notification);
    }

    public void executeShellCommand(String userCommand) {
        new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();

                processBuilder.environment().put("TMPDIR", TermuxService.PREFIX_PATH + "/tmp");
                processBuilder.environment().put("PATH", TermuxService.PREFIX_PATH + "/bin");
                processBuilder.environment().put("LD_LIBRARY_PATH", TermuxService.PREFIX_PATH + "/libs");
                processBuilder.environment().put("HOME", TermuxService.HOME_PATH);

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
                    MainActivity.activity.runOnUiThread(() -> MainActivity.tvLogger.append(outputLine + "\n"));
                }

                // Read any errors from the error stream
                while ((line = errorReader.readLine()) != null) {
                    final String errorLine = line;
                    MainActivity.activity.runOnUiThread(() -> MainActivity.tvLogger.append(errorLine + "\n"));
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
                    String exitMessage = "Command failed with exit code: " + exitValue;
                    MainActivity.activity.runOnUiThread(() -> MainActivity.tvLogger.append(exitMessage + "\n"));
                }
            } catch (IOException | InterruptedException e) {
                // Handle exceptions by printing the stack trace in the terminal output
                final String errorMessage = e.getMessage();
                MainActivity.activity.runOnUiThread(() -> MainActivity.tvLogger.append(errorMessage + "\n"));
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_ID,
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}