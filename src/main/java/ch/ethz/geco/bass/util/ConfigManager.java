package ch.ethz.geco.bass.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.*;

public class ConfigManager {
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static JsonObject config;

    private static void save() {
        try {
            JsonWriter jw = new JsonWriter(new FileWriter("./config.json"));
            jw.setIndent("  ");

            gson.toJson(config, jw);
            jw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void load() {
        try {
            JsonReader jr = new JsonReader(new FileReader("./config.json"));
            JsonParser jp = new JsonParser();

            config = jp.parse(jr).getAsJsonObject();

            jr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Get and set of configs here:

    public static String getKeystorePassword() {
        if (config == null)
            load();

        return config.get("KeystorePassword").getAsString();
    }

    public static String getKeyPassword() {
        if (config == null)
            load();

        return config.get("KeyPassword").getAsString();
    }
}
