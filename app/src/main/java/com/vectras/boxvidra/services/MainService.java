package com.vectras.boxvidra.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.system.ErrnoException;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.termux.app.TermuxService;
import com.vectras.boxvidra.R;
import com.vectras.boxvidra.activities.MainActivity;
import com.vectras.boxvidra.core.SoundThread;
import com.vectras.boxvidra.core.TermuxX11;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.app.PendingIntent;

public class MainService extends Service {
    private static final String TAG = "MainService";
    private static final String CHANNEL_ID = "MainServiceChannel";
    private static final int NOTIFICATION_ID = 12345;

    private SoundThread soundThread;
    private Thread thread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Boxvidra")
                .setContentText("Boxvidra running in background")
                .setSmallIcon(R.drawable.ic_main_service_icon)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        String prootCommand = "su - xuser";
        String prootPath = TermuxService.PREFIX_PATH + "/bin/proot";
        executeProotCommand(prootPath, prootCommand);
        Intent x11Intent = new Intent();
        x11Intent.setClassName("com.termux.x11", "com.termux.x11.MainActivity");
        x11Intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(x11Intent);
        } catch (ActivityNotFoundException e) {
            Log.e("LaunchActivity", "Activity not found: " + e.getMessage());
        }

        //startAudio(ip, port);

        return START_NOT_STICKY;
    }

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getHostAddress().toString().contains(".")) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private void startAudio(String ip, String port) {
        if (soundThread == null) {
            soundThread = new SoundThread(ip, port, error -> {
                // Handle errors here
                Log.e(TAG, "Error: " + error);
            });
            thread = new Thread(soundThread);
            thread.start();
            Log.d(TAG, "Audio started");
        }
    }

    private void stopAudio() {
        if (soundThread != null) {
            soundThread.Terminate();
            soundThread = null;
            thread = null;
            Log.d(TAG, "Audio stopped");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
    }

    private void executeProotCommand(String prootPath, String command) {
        new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();

                String[] prootCommand = {
                        prootPath,
                        "--link2symlink",
                        "-0",
                        "-r", TermuxService.HOME_PATH + "/debian-fs",
                        "--bind=/dev",
                        "--bind=/proc",
                        "--bind=" + TermuxService.HOME_PATH + "/debian-fs/root:/dev/shm",
                        "--bind=/sdcard",
                        "--bind=/data",
                        "--bind=/data/data/com.vectras.boxvidra/files/usr/tmp:/tmp",
                        "-w", "/root",
                        "/usr/bin/env", "-i",
                        "HOME=/root",
                        "PATH=/usr/local/sbin:/usr/local/bin:/bin:/usr/bin:/sbin:/usr/sbin:/usr/games:/usr/local/games",
                        "LANG=C.UTF-8",
                        "DISPLAY=:0",
                        "LANG=en_US.UTF-8",
                        "MOZ_FAKE_NO_SANDBOX=1",
                        "PULSE_SERVER=127.0.0.1",
                        "TERM=" + System.getenv("TERM"),
                        "TMPDIR=/tmp",
                        "WINEPREFIX=" + TermuxService.OPT_PATH + "/wine-prefixes/",
                        "/bin/bash",
                        "-c",
                        command
                };

                processBuilder.command(prootCommand);

                Process process = processBuilder.start();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                // Send command to proot
                writer.write(command);
                writer.newLine();
                writer.flush();
                writer.close();

                String line;
                while ((line = reader.readLine()) != null) {
                    Log.d(TAG, line); // Log output from proot
                }

                // Read any errors from the error stream
                while ((line = errorReader.readLine()) != null) {
                    Log.e(TAG, line); // Log errors from proot
                }

                // Wait for the process to finish
                int exitValue = process.waitFor();
                Log.d(TAG, "Command execution finished with exit code: " + exitValue);

                // Clean up
                reader.close();
                errorReader.close();

            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "Error executing proot command: " + e.getMessage());
            }
        }).start();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "MainService Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
