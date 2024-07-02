package com.vectras.boxvidra.utils;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {

    public static void copyFolderFromAssets(Context context, String assetsFolderPath, String destinationPath) {
        try {
            String[] files = context.getAssets().list(assetsFolderPath);

            if (files != null) { // it's a folder
                new File(destinationPath).mkdirs();
                for (String file : files) {
                    copyFolderFromAssets(context, assetsFolderPath + "/" + file, destinationPath + "/" + file);
                }
            } else { // it's a file
                copyFile(context, assetsFolderPath, destinationPath);
            }
        } catch (IOException e) {
            Log.e("FileUtils", "Error copying folder from assets", e);
        }
    }

    private static void copyFile(Context context, String assetsFilePath, String destinationPath) throws IOException {
        InputStream in = context.getAssets().open(assetsFilePath);
        new File(destinationPath).getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(destinationPath);

        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }

        in.close();
        out.flush();
        out.close();
    }
}
