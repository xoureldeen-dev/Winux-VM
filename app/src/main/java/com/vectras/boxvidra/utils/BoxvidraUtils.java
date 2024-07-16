package com.vectras.boxvidra.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.termux.app.TermuxService;
import com.vectras.boxvidra.fragments.EnvironmentVariablesFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class BoxvidraUtils {
    public static String prefixName = null;

    public static String BoxvidraCmdLine(Context context) {
        if (prefixName == null)
            return null;

        SharedPreferences sharedPreferences = context.getSharedPreferences(EnvironmentVariablesFragment.PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> savedVars = sharedPreferences.getStringSet(EnvironmentVariablesFragment.ENVIRONMENT_VARS_KEY, new HashSet<>());
        ArrayList<String> environmentVariables = new ArrayList<>(savedVars);

        ArrayList<String> command = new ArrayList<>();

        for (String var : environmentVariables) {
            command.add("export " + var);
        }

        File prefixDir = new File(TermuxService.OPT_PATH + "/wine-prefixes/" + prefixName);

        command.add("export DISPLAY=:0");
        command.add("export PULSE_SERVER=tcp:127.0.0.1");
        command.add("export WINEPREFIX='" + prefixDir.getAbsolutePath() + "'");
        command.add(";");

        // Retrieve command options from JSON
        File optionsFile = new File(prefixDir, "options.json");
        try {
            if (optionsFile.exists()) {
                JSONObject options = JsonUtils.loadOptionsFromJson(optionsFile);
                if (options.getBoolean("wine64")) {
                    command.add("wine64 explorer /desktop=shell,1024x786");
                }
                if (options.getBoolean("startxfce4")) {
                    command.add("startxfce4");
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return String.join(" ", command);
    }
}
