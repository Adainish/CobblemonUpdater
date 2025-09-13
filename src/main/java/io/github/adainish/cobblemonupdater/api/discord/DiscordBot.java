package io.github.adainish.cobblemonupdater.api.discord;

import io.github.adainish.cobblemonupdater.Cobblemonupdater;
import io.github.adainish.cobblemonupdater.api.Logger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that represents a Discord bot.
 *
 * @Author Adainish
 */
public class DiscordBot extends AbstractDiscordBot implements IDiscordBot {
    /**
     * This'll be the URL to check for the latest version of Cobblemon
     * From here we can scan for the right jar build
     * E.g. https://www.curseforge.com/minecraft/mc-mods/cobblemon/files/all?filter-game-version=2020709689%3A7495
     * We need to make sure we always get the latest version for the right MC version
     * So we can parse the page for the right download link
     */

    public String cobblemonVersionURL = "";

    /**
     * Default constructor
     */
    public DiscordBot() {
        super();
    }

    /**
     * Constructor with token
     *
     * @param token The token to log in with
     */
    public DiscordBot(String token) {
        super();
        this.setToken(token);
    }

    /**
     * Log in to the bot
     * @param args Token to log in with
     * @throws Exception If login fails
     */
    @Override
    public void login(String args) throws Exception {
        if (args.isEmpty()) {
            Logger.log("No token provided, Discord bot did not log in.");
            return;
        }
        try {

            JDABuilder jdaBuilder = JDABuilder.createDefault(args);
            jdaBuilder.setStatus(OnlineStatus.ONLINE);
            jdaBuilder.setActivity(Activity.playing("Cobblemon Updater v1.0"));
            jdaBuilder.addEventListeners(this);
            JDA jda = jdaBuilder.build();
            jda.awaitReady();
            jda.upsertCommand(Commands.slash("cobblemonstatus", "Check the Cobblemon version status")).queue();
            jda.upsertCommand(Commands.slash("setupdateurl", "Set the Cobblemon update URL")
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "url", "The update URL", true)).queue();
            jda.upsertCommand(
                    Commands.slash("updatecobblemon", "Manually trigger a Cobblemon update")
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.BOOLEAN, "ignore_mc_version", "Ignore MC version and grab highest Cobblemon version", false)
            ).queue();
            this.setJDA(jda);
            this.cobblemonVersionURL = Cobblemonupdater.config.cobblemonUpdateURL;
            Logger.log("Discord bot logged in successfully!");
            this.setToken(args);
        } catch (Exception e) {
            Logger.log(e.getMessage());
            throw new Exception("Failed to log in");
        }

    }

    private String normalizeVersion(String version) {
        if (version == null) return null;
        int plusIndex = version.indexOf('+');
        return plusIndex > 0 ? version.substring(0, plusIndex) : version;
    }


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "cobblemonstatus" -> {
                String currentVersion = getCurrentCobblemonVersion();
                String latestVersion = getLatestCobblemonVersionFromURL();
                boolean upToDate = false;
                if (currentVersion != null && latestVersion != null) {
                    upToDate = normalizeVersion(currentVersion).equals(normalizeVersion(latestVersion));
                }
                String mcVersion = getCurrentMinecraftVersion();
                String reply = "Current Minecraft version: " + (mcVersion != null ? mcVersion : "Unknown") +
                        "\nCurrent Cobblemon version: " + (currentVersion != null ? currentVersion : "Unknown") +
                        "\nLatest Cobblemon version: " + (latestVersion != null ? latestVersion : "Unknown") +
                        "\nServer is " + (upToDate ? "up to date ✅" : "out of date ❌");
                event.reply(reply).queue();
            }
            case "setupdateurl" -> {
                String url = event.getOption("url").getAsString();
                this.cobblemonVersionURL = url;
                //update in config
                Cobblemonupdater.config.cobblemonUpdateURL = url;
                Cobblemonupdater.config.saveConfig();
                event.reply("Cobblemon update URL set to: " + url).setEphemeral(true).queue();
            }
            case "updatecobblemon" -> {
                boolean ignoreMcVersion = false;
                if (event.getOption("ignore_mc_version") != null) {
                    ignoreMcVersion = event.getOption("ignore_mc_version").getAsBoolean();
                }
                event.reply("Starting Cobblemon update...").setEphemeral(true).queue();
                long userId = event.getUser().getIdLong();
                boolean finalIgnoreMcVersion = ignoreMcVersion;
                new Thread(() -> updateCobblemon(userId, finalIgnoreMcVersion)).start();
            }
            case "reloadconfig" -> {
                try {
                    Cobblemonupdater.instance.reload();
                    event.reply("Config files reloaded successfully.").setEphemeral(true).queue();
                } catch (Exception e) {
                    event.reply("Failed to reload config files: " + e.getMessage()).setEphemeral(true).queue();
                }
            }
        }
    }


    /**
     * Broadcasts a message to a channel
     *
     * @param title        Title of the message
     * @param message      Message to send
     * @param channel      Channel to send message to
     * @param pingEveryone Ping everyone in the channel
     */
    public void broadcastWithTitle(String title, String message, String channel, boolean pingEveryone) {
        this.broadcast(title, "\n" + message, channel, pingEveryone);
    }

    public String getLatestCobblemonVersionFromURL() {
        String latestVersion = "unknown";
        if (this.cobblemonVersionURL == null || this.cobblemonVersionURL.isEmpty()) {
            return latestVersion;
        }
        // Try Maven first
        latestVersion = getLatestCobblemonVersionURL(this.cobblemonVersionURL);
        if (latestVersion == null || latestVersion.equals("unknown")) {
            // Fallback: Try Modrinth API
            try (InputStream in = new URL("https://api.modrinth.com/v2/project/cobblemon/version").openStream()) {
                StringBuilder json = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        json.append(line);
                    }
                }
                String jsonString = json.toString();
                int idx = jsonString.indexOf("\"version_number\":\"");
                if (idx != -1) {
                    int start = idx + 18;
                    int end = jsonString.indexOf("\"", start);
                    if (end > start) {
                        latestVersion = jsonString.substring(start, end);
                    }
                }
            } catch (Exception e) {
                Logger.log(e);
                // Both Maven and Modrinth failed
                latestVersion = "No servers available for downloading";
            }
        }
        return latestVersion;
    }



    public String getLatestCobblemonVersionURL(String repoBaseUrl) {
        String group = "com.cobblemon";
        String artifact = "fabric";
        String metadataUrl = repoBaseUrl;
        if (!metadataUrl.endsWith("/")) metadataUrl += "/";
        metadataUrl += group.replace('.', '/') + "/" + artifact + "/maven-metadata.xml";
        String latestVersion = "unknown";
        try (InputStream in = new URL(metadataUrl).openStream()) {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
            doc.getDocumentElement().normalize();
            NodeList latestList = doc.getElementsByTagName("latest");
            if (latestList.getLength() > 0) {
                latestVersion = latestList.item(0).getTextContent();
            } else {
                NodeList versions = doc.getElementsByTagName("version");
                if (versions.getLength() > 0) {
                    latestVersion = versions.item(versions.getLength() - 1).getTextContent();
                }
            }
        } catch (Exception e) {
            Logger.log(e);
        }
        return latestVersion;
    }

    public String getCurrentMinecraftVersion() {
        try {
            return FabricLoader.getInstance().getModContainer("minecraft")
                    .flatMap(mc -> mc.getMetadata().getVersion().getFriendlyString() != null
                            ? java.util.Optional.of(mc.getMetadata().getVersion().getFriendlyString())
                            : java.util.Optional.empty())
                    .orElse("unknown");
        } catch (Exception e) {
            Logger.log(e);
            return "unknown";
        }
    }

    //function to get the current cobblemon version installed on the mc server
    public String getCurrentCobblemonVersion() throws RuntimeException {
        String version = "unknown";
        try {
            if (Class.forName("net.fabricmc.loader.api.FabricLoader") != null) {
                Object fabricLoader = Class.forName("net.fabricmc.loader.api.FabricLoader")
                        .getMethod("getInstance").invoke(null);
                Object optional = fabricLoader.getClass()
                        .getMethod("getModContainer", String.class).invoke(fabricLoader, "cobblemon");
                Class<?> optionalClass = Class.forName("java.util.Optional");
                boolean isPresent = (boolean) optionalClass.getMethod("isPresent").invoke(optional);
                if (isPresent) {
                    Object modContainer = optionalClass.getMethod("get").invoke(optional);
                    Object metadata = modContainer.getClass().getMethod("getMetadata").invoke(modContainer);
                    java.lang.reflect.Method getVersionMethod = metadata.getClass().getMethod("getVersion");
                    getVersionMethod.setAccessible(true); // <-- This line is key
                    Object versionObj = getVersionMethod.invoke(metadata);
                    version = versionObj.toString();
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return version;
    }



    public String currentVersions() {
        return "Minecraft: " + this.getCurrentMinecraftVersion() + ", Cobblemon: " + this.getCurrentCobblemonVersion();
    }

    public void sendCurrentVersionsToServerPlayer(ServerPlayer player) {
        String versions = this.currentVersions();
        player.sendSystemMessage(Component.literal(versions));
    }

    public void sendCurrentVersionsToDiscordUser(long userID) {
        String versions = this.currentVersions();
        this.sendDiscordDM(userID, versions);
    }

    // Add this helper to extract the MC version from a Cobblemon version string
    private String extractMinecraftVersion(String cobblemonVersion) {
        if (cobblemonVersion == null) return null;
        int plusIndex = cobblemonVersion.indexOf('+');
        return (plusIndex > 0 && plusIndex < cobblemonVersion.length() - 1)
                ? cobblemonVersion.substring(plusIndex + 1)
                : null;
    }

    // Helper to compare semantic versions
    private int compareVersions(String v1, String v2) {
        String[] a1 = v1.split("\\.");
        String[] a2 = v2.split("\\.");
        int len = Math.max(a1.length, a2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < a1.length ? Integer.parseInt(a1[i].replaceAll("\\D.*", "")) : 0;
            int n2 = i < a2.length ? Integer.parseInt(a2[i].replaceAll("\\D.*", "")) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    public void updateCobblemon(long userID, boolean ignoreMcVersion) {
        String latestVersion = this.getLatestCobblemonVersionFromURL();
        String currentVersion = this.getCurrentCobblemonVersion();

        if (latestVersion == null || latestVersion.equals("unknown") || latestVersion.equals("No servers available for downloading")) {
            this.sendDiscordDM(userID, "Unable to determine the latest version of Cobblemon.");
            return;
        }

        String latestMcVersion = extractMinecraftVersion(latestVersion);
        String currentMcVersion = getCurrentMinecraftVersion();
        if (latestMcVersion != null && !latestMcVersion.equals(currentMcVersion)) {
            this.sendDiscordDM(userID, "Latest Cobblemon version (" + latestVersion + ") is for Minecraft " + latestMcVersion +
                    ", but the server is running Minecraft " + currentMcVersion + ". Update aborted.");
            return;
        }

        if (normalizeVersion(currentVersion).equals(normalizeVersion(latestVersion))) {
            this.sendDiscordDM(userID, "Cobblemon is up to date! Current version: " + currentVersion);
            return;
        }

        String group = "com.cobblemon";
        String artifact = "fabric";
        String mavenBase = this.cobblemonVersionURL;
        if (!mavenBase.endsWith("/")) mavenBase += "/";
        String mavenJarUrl = mavenBase + group.replace('.', '/') + "/" + artifact + "/" + latestVersion + "/" + artifact + "-" + latestVersion + ".jar";
        boolean downloaded = false;

        // Try Maven first
        try (InputStream in = new URL(mavenJarUrl).openStream()) {
            Path modsDir = Paths.get("mods");
            Files.createDirectories(modsDir);
            Path tempFile = modsDir.resolve("cobblemon-latest.jar");
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            downloaded = true;
        } catch (Exception e) {
            Logger.log("Maven download failed, trying Modrinth...");
        }

        // Fallback: Try Modrinth
        if (!downloaded) {
            try (InputStream in = new URL("https://api.modrinth.com/v2/project/cobblemon/version").openStream()) {
                StringBuilder json = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                    String line;
                    while ((line = reader.readLine()) != null) json.append(line);
                }
                String jsonString = json.toString();
                class FabricJar {
                    String version;
                    String url;
                    String filename;
                    String mcVersion;
                }
                List<FabricJar> fabricJars = new ArrayList<>();
                int idx = 0;
                while ((idx = jsonString.indexOf("\"filename\":\"", idx)) != -1) {
                    int fnameStart = idx + 12;
                    int fnameEnd = jsonString.indexOf("\"", fnameStart);
                    if (fnameEnd == -1) break;
                    String filename = jsonString.substring(fnameStart, fnameEnd);
                    if (filename.contains("fabric")) {
                        String version = "0.0.0";
                        String mcVersion = null;
                        int dashIdx = filename.indexOf("fabric-");
                        int plusIdx = filename.indexOf("+", dashIdx);
                        int dotJarIdx = filename.indexOf(".jar", plusIdx);
                        if (dashIdx != -1 && plusIdx != -1 && dotJarIdx != -1) {
                            version = filename.substring(dashIdx + 7, plusIdx);
                            mcVersion = filename.substring(plusIdx + 1, dotJarIdx);
                        }
                        int urlIdx = jsonString.indexOf("\"url\":\"", fnameEnd);
                        if (urlIdx != -1) {
                            int urlStart = urlIdx + 7;
                            int urlEnd = jsonString.indexOf("\"", urlStart);
                            if (urlEnd != -1) {
                                String modrinthJarUrl = jsonString.substring(urlStart, urlEnd);
                                FabricJar jar = new FabricJar();
                                jar.version = version;
                                jar.url = modrinthJarUrl;
                                jar.filename = filename;
                                jar.mcVersion = mcVersion;
                                fabricJars.add(jar);
                            }
                        }
                    }
                    idx = fnameEnd;
                }
                // Filter by MC version if not ignoring
                List<FabricJar> matching = new ArrayList<>();
                if (ignoreMcVersion) {
                    matching.addAll(fabricJars);
                } else {
                    for (FabricJar jar : fabricJars) {
                        if (jar.mcVersion != null && jar.mcVersion.equals(currentMcVersion)) {
                            matching.add(jar);
                        }
                    }
                }
                if (matching.isEmpty()) {
                    Logger.log("No fabric jar found" + (ignoreMcVersion ? "" : " for MC version " + currentMcVersion) + " in Modrinth response.");
                } else {
                    Logger.log("Available Fabric jars" + (ignoreMcVersion ? "" : " for MC version " + currentMcVersion) + ":");
                    for (FabricJar jar : matching) {
                        Logger.log("Filename: " + jar.filename + ", Version: " + jar.version + ", MC: " + jar.mcVersion + ", URL: " + jar.url);
                    }
                    // Find the highest version
                    FabricJar latest = matching.get(0);
                    for (FabricJar jar : matching) {
                        if (compareVersions(jar.version, latest.version) > 0) {
                            latest = jar;
                        }
                    }
                    Logger.log("Downloading version: " + latest.version + " from: " + latest.url);
                    try (InputStream jarIn = new URL(latest.url).openStream()) {
                        Path modsDir = Paths.get("mods");
                        Files.createDirectories(modsDir);
                        Path tempFile = modsDir.resolve("cobblemon-latest.jar");
                        Files.copy(jarIn, tempFile, StandardCopyOption.REPLACE_EXISTING);
                        downloaded = true;
                    }
                }
            } catch (Exception e) {
                Logger.log("Modrinth download failed.");
            }
        }


        if (downloaded) {
            try {
                Path modsDir = Paths.get("mods");
                String newJarName = "Cobblemon-fabric-" + normalizeVersion(latestVersion) + "+" + getCurrentMinecraftVersion() + ".jar";
                Path newJarPath = modsDir.resolve(newJarName);

                try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "Cobblemon-fabric-*.jar")) {
                    for (Path path : stream) {
                        if (!path.getFileName().toString().equals(newJarName)) {
                            Files.deleteIfExists(path);
                        }
                    }
                }

                Files.move(modsDir.resolve("cobblemon-latest.jar"), newJarPath, StandardCopyOption.REPLACE_EXISTING);

                this.sendDiscordDM(userID, "Downloaded and replaced Cobblemon with version: " + latestVersion);
                this.sendDiscordDM(userID, "Rebooting the Minecraft server to apply the update...");
                try { Thread.sleep(5000); } catch (InterruptedException e) { /*ignore*/ }
                MinecraftServer server = Cobblemonupdater.getServer();
                if (server != null) server.halt(true);
            } catch (Exception e) {
                Logger.log(e);
                this.sendDiscordDM(userID, "Failed to replace Cobblemon jar.");
            }
        } else {
            this.sendDiscordDM(userID, "Could not download the latest Cobblemon jar from any source.");
        }
    }


}
