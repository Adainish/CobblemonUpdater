package io.github.adainish.cobblemonupdater;

import io.github.adainish.cobblemonupdater.api.discord.DiscordBot;
import io.github.adainish.cobblemonupdater.api.Logger;
import io.github.adainish.cobblemonupdater.config.Config;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;

import java.io.File;

@Mod("cobblemonupdater")
public class Cobblemonupdater {
    public static Cobblemonupdater instance;
    public MinecraftServer server;
    private static File configDir;
    private static File storage;
    public static String token;
    public static DiscordBot bot;
    public static Config config;

    public Cobblemonupdater() {
        instance = this;
        // Register event handlers
        NeoForge.EVENT_BUS.register(this);
    }

    public static File getConfigDir() {
        return configDir;
    }

    public static void setConfigDir(File configDir) {
        Cobblemonupdater.configDir = configDir;
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        this.server = event.getServer();
        Logger.log("Server started");
        load();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        logoutBot();
    }

    public void load() {
        initDirs();
        initConfigs();
        if (!loginBot()) {
            Logger.log("Failed to login to bot, please check your token in the config file");
        }
    }

    public void reload() {
        initConfigs();
        logoutBot();
        if (!loginBot()) {
            Logger.log("Failed to login to bot, please check your token in the config file");
        }
    }

    public static MinecraftServer getServer() {
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
        setConfigDir(FMLPaths.CONFIGDIR.get().resolve("CobblemonUpdater").toFile());
        getConfigDir().mkdir();
    }

    public void initConfigs() {
        Config.writeConfig();
        config = Config.getConfig();
    }
}
