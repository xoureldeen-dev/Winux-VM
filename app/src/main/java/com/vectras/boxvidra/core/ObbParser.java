package com.vectras.boxvidra.core;
import android.app.Activity;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ObbParser {

    public static JSONObject obbParse(Activity activity) throws IOException, JSONException {
        String filesDir = activity.getFilesDir().getAbsolutePath();
        String jsonString = new String(Files.readAllBytes(Paths.get(filesDir + "/DISTRO-INFO.json")));
        return new JSONObject(jsonString);
    }

    public static String parseJsonAndFormat(JSONObject jsonObject) {
        try {
            String distroName = jsonObject.getString("distroName");
            String distroVersion = jsonObject.getString("distroVersion");
            String wineVersion = jsonObject.getString("wineVersion");
            String desktopEnvironment = jsonObject.getString("desktopEnvironment");
            String obbVersion = jsonObject.getString("obbVersion");

            return String.format("Distro Name: %s\nDistro Version: %s\nDesktop Environment: %s\nWine Version: %s\nOBB Version: %s",
                    distroName, distroVersion, desktopEnvironment, wineVersion, obbVersion);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static String wineVersion(Activity activity) throws IOException, JSONException {
        String filesDir = activity.getFilesDir().getAbsolutePath();
        String jsonString = new String(Files.readAllBytes(Paths.get(filesDir + "/DISTRO-INFO.json")));
        JSONObject jsonObject = new JSONObject(jsonString);
        try {
            return jsonObject.getString("wineVersion");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}