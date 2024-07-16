package com.vectras.boxvidra.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.termux.app.TermuxService;
import com.vectras.boxvidra.R;
import com.vectras.boxvidra.core.SoundThread;
import com.vectras.boxvidra.utils.BoxvidraUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class MainService extends Service {
    private static final String TAG = "MainService";
    private static final String CHANNEL_ID = "MainServiceChannel";
    private static final int NOTIFICATION_ID = 12345;

    private SoundThread soundThread;
    private Thread thread;

    private Process prootProcess;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Intent killAllIntent = new Intent(this, MainService.class);
        killAllIntent.setAction("KILL_ALL");
        PendingIntent killAllPendingIntent = PendingIntent.getService(this, 0, killAllIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Boxvidra")
                .setContentText("Pull down to show options")
                .setSmallIcon(R.drawable.ic_main_service_icon)
                .addAction(R.drawable.round_logout_24, "Stop All Processes", killAllPendingIntent);

        Notification notification = notificationBuilder.build();

        startForeground(NOTIFICATION_ID, notification);

        String commandLine = BoxvidraUtils.BoxvidraCmdLine(getApplicationContext());
        if (commandLine != null) {
            executeProotCommand("virgl_test_server_android");
            executeProotCommand(commandLine);
        } else {
            stopSelf();
        }

        Intent x11Intent = new Intent();
        x11Intent.setClassName("com.termux.x11", "com.termux.x11.MainActivity");
        x11Intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(x11Intent);
        } catch (ActivityNotFoundException e) {
            Log.e("LaunchActivity", "Activity not found: " + e.getMessage());
        }

        if (intent != null && "KILL_ALL".equals(intent.getAction())) {
            stopAllProcesses();
        }

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
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

    private void startAudio(String ip, String port) {
        if (soundThread == null) {
            soundThread = new SoundThread(ip, port, error -> {
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

    private void executeProotCommand(String command) {
        new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();

                String[] prootCommand = {
                        TermuxService.PREFIX_PATH + "/bin/proot",
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
                        "LANG=en_US.UTF-8",
                        "DISPLAY=:0",
                        "MOZ_FAKE_NO_SANDBOX=1",
                        "TERM=" + System.getenv("TERM"),
                        "TMPDIR=/tmp",
                        "/bin/bash",
                        "--login"
                };

                processBuilder.command(prootCommand);

                prootProcess = processBuilder.start();
                Log.d(TAG, "Proot process started");

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(prootProcess.getOutputStream()));
                BufferedReader reader = new BufferedReader(new InputStreamReader(prootProcess.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(prootProcess.getErrorStream()));

                writer.write(command);
                writer.newLine();
                writer.flush();

                String line;
                while ((line = reader.readLine()) != null) {
                    Log.d(TAG, line);
                }

                while ((line = errorReader.readLine()) != null) {
                    Log.e(TAG, line);
                }

                int exitValue = prootProcess.waitFor();
                Log.d(TAG, "Command execution finished with exit code: " + exitValue);

                writer.close();
                reader.close();
                errorReader.close();

            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "Error executing proot command: " + e.getMessage());
            }
        }).start();
    }

    private void stopAllProcesses() {
        stopAudio();
        stopSelf();
        System.exit(0);
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
}

