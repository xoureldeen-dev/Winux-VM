package com.vectras.boxvidra.utils;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;

public class JsonUtils {

    public static void saveOptionsToJson(File file, JSONObject jsonObject) throws IOException {
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(jsonObject.toString());
        }
    }

    public static JSONObject loadOptionsFromJson(File file) throws IOException {
        StringBuilder jsonContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
        }
        try {
            return new JSONObject(jsonContent.toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
