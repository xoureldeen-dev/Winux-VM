package com.vectras.boxvidra.core;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class SoundThread implements Runnable {
    private static final String TAG = "SoundThread";
    private volatile boolean mTerminate = false;
    private final String mServer;
    private final int mPort;
    private final Callback callback;

    public interface Callback {
        void onError(String error);
    }

    public SoundThread(String server, String port, Callback callback) {
        this.mServer = server;
        this.mPort = Integer.parseInt(port);
        this.callback = callback;
    }

    public void Terminate() {
        mTerminate = true;
    }

    @Override
    public void run() {
        Socket sock = null;
        BufferedInputStream audioData = null;
        try {
            sock = new Socket(mServer, mPort);
            callback.onError("Connected");
        } catch (UnknownHostException e) {
            Terminate();
            e.printStackTrace();
            Log.e(TAG, "Unknown Host: " + e);
            callback.onError("Unknown Host: " + e);
        } catch (IOException e) {
            Terminate();
            e.printStackTrace();
            Log.e(TAG, "IOException: " + e);
            callback.onError(e.getMessage());
        } catch (SecurityException e) {
            Terminate();
            e.printStackTrace();
            Log.e(TAG, "Security: " + e);
            callback.onError(e.getMessage());
        }

        if (!mTerminate) {
            try {
                audioData = new BufferedInputStream(sock.getInputStream());
            } catch (IOException e) {
                Terminate();
                e.printStackTrace();
                callback.onError(e.getMessage());
            }
        }

        // Create AudioPlayer
        int sampleRate = 48000;
        int musicLength = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                musicLength,
                AudioTrack.MODE_STREAM
        );
        audioTrack.play();

        byte[] audioBuffer = new byte[musicLength * 8];

        while (!mTerminate) {
            try {
                int sizeRead = audioData.read(audioBuffer, 0, musicLength * 8);
                int sizeWrite = audioTrack.write(audioBuffer, 0, sizeRead);
                if (sizeWrite == AudioTrack.ERROR_INVALID_OPERATION) {
                    sizeWrite = 0;
                }
                if (sizeWrite == AudioTrack.ERROR_BAD_VALUE) {
                    sizeWrite = 0;
                }
            } catch (IOException e) {
                e.printStackTrace();
                callback.onError(e.getMessage());
            }
        }

        audioTrack.stop();
        audioTrack.release();
        try {
            if (sock != null) {
                sock.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
