package io.github.adainish.cobblemonupdater;

import io.github.adainish.cobblemonupdater.api.discord.DiscordBot;
import io.github.adainish.cobblemonupdater.api.Logger;
import io.github.adainish.cobblemonupdater.config.Config;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.io.File;

public class Cobblemonupdater implements ModInitializer {
    public static Cobblemonupdater instance;
    public MinecraftServer server;
    private static File configDir;
    private static File storage;
    public static String token;
    public static DiscordBot bot;
    public static Config config;
    public static File getConfigDir() {
        return configDir;
    }

    public static void setConfigDir(File configDir) {
        Cobblemonupdater.configDir = configDir;
    }
    @Override
    public void onInitialize() {
        instance = this;

        //set the server variable when the server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            Logger.log("Server started");
            load();
        }
        );
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            logoutBot();
        });
    }

    public void load()
    {
        initDirs();
        initConfigs();
        if (!loginBot())
        {
            Logger.log("Failed to login to bot, please check your token in the config file");
        }
    }

    public void reload()
    {
        initConfigs();
        logoutBot();
        if (!loginBot())
        {
            Logger.log("Failed to login to bot, please check your token in the config file");
        }
    }

    public static MinecraftServer getServer()
    {
        return instance.server;
    }

    public boolean loginBot() {
        token = config.botToken;

        bot = new DiscordBot();
        if (token == null || token.isEmpty()) {
            Logger.log("There was an issue logging into the Bot, The Token was either null or empty!");
            return false;
        }
        try {
            bot.login(token);
        } catch (Exception e) {
            Logger.log(e.getMessage());
            Logger.log(e);
            return false;
        }
        return true;
    }

    public void logoutBot() {
        if (bot == null)
            return;
        bot.logout();

    }


    public void initDirs() {
        setConfigDir(new File(FabricLoader.getInstance().getConfigDir() + "/CobblemonUpdater/"));
        getConfigDir().mkdir();
    }

    public void initConfigs() {
        Config.writeConfig();
        config = Config.getConfig();
    }

}
