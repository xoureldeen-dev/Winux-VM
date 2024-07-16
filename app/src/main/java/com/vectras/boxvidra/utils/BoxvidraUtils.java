package com.vectras.boxvidra.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.termux.app.TermuxService;
import com.vectras.boxvidra.fragments.EnvironmentVariablesFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BoxvidraUtils {
    public static String prefixName = null;

    public static String BoxvidraCmdLine(Context context) {
        if (prefixName == null)
            return null;

        // Retrieve saved environment variables from SharedPreferences
        SharedPreferences sharedPreferences = context.getSharedPreferences(EnvironmentVariablesFragment.PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> savedVars = sharedPreferences.getStringSet(EnvironmentVariablesFragment.ENVIRONMENT_VARS_KEY, new HashSet<>());
        ArrayList<String> environmentVariables = new ArrayList<>(savedVars);

        // Build the command with environment variables
        ArrayList<String> command = new ArrayList<>();

        for (String var : environmentVariables) {
            command.add("export " + var);
        }

        command.add("export WINEDEBUG=+all");
        command.add("export DISPLAY=:0");
        command.add("export PULSE_SERVER=tcp:127.0.0.1");
        command.add("export WINEPREFIX='" + TermuxService.OPT_PATH + "/wine-prefixes/" + prefixName + "'");
        command.add(";");

        command.add("xfce4-session");

        return String.join(" ", command);
    }
}


