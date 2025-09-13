package io.github.adainish.cobblemonupdater.config;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import io.github.adainish.cobblemonupdater.Cobblemonupdater;
import io.github.adainish.cobblemonupdater.api.Logger;

import java.io.*;

public class Config
{
    public String guildID;
    public String botToken;
    public String channelID;
    public String cobblemonUpdateURL;

    public Config()
    {
        this.botToken = "";
        this.channelID = "";
        this.guildID = "";
        this.cobblemonUpdateURL = "https://maven.impactdev.net/repository/development/";
    }

    public static void writeConfig()
    {
        File dir = Cobblemonupdater.getConfigDir();
        dir.mkdirs();
        Gson gson  = Adapters.PRETTY_MAIN_GSON;
        Config config = new Config();
        try {
            File file = new File(dir, "config.json");
            if (file.exists())
                return;
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            String json = gson.toJson(config);
            writer.write(json);
            writer.close();
        } catch (IOException e)
        {
            Logger.log(e);
        }
    }

    public static Config getConfig()
    {
        File dir = Cobblemonupdater.getConfigDir();
        dir.mkdirs();
        Gson gson  = Adapters.PRETTY_MAIN_GSON;
        File file = new File(dir, "config.json");
        JsonReader reader = null;
        try {
            reader = new JsonReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            Logger.log("Something went wrong attempting to read the Config");
            return null;
        }

        return gson.fromJson(reader, Config.class);
    }

    public void saveConfig() {
        File dir = Cobblemonupdater.getConfigDir();
        dir.mkdirs();
        Gson gson  = Adapters.PRETTY_MAIN_GSON;
        try {
            File file = new File(dir, "config.json");
            if (!file.exists())
                file.createNewFile();
            FileWriter writer = new FileWriter(file);
            String json = gson.toJson(this);
            writer.write(json);
            writer.close();
        } catch (IOException e)
        {
            Logger.log(e);
        }
    }
}
