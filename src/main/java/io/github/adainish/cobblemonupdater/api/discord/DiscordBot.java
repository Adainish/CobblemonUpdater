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
            Logger.log("Registering /cobblemonstatus command");
            jda.upsertCommand(Commands.slash("cobblemonstatus", "Check the Cobblemon version status")
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.BOOLEAN, "use_snapshots", "Use snapshot builds instead of releases", false))
                    .queue();

            Logger.log("Registering /setupdateurl command");
            jda.upsertCommand(Commands.slash("setupdateurl", "Set the Cobblemon update URL")
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "url", "The update URL", true)).queue();

            Logger.log("Registering /updatespecified command");
            jda.upsertCommand(
                    Commands.slash("updatespecified", "Update Cobblemon with a specific jar from a URL")
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "url", "Direct URL to the jar", true)
            ).queue();

            Logger.log("Registering /updatecobblemon command");
            jda.upsertCommand(
                    Commands.slash("updatecobblemon", "Manually trigger a Cobblemon update")
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.BOOLEAN, "ignore_mc_version", "Ignore MC version and grab highest Cobblemon version", false)
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.BOOLEAN, "use_snapshots", "Use snapshot builds instead of releases", false)
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
                boolean useSnapshots = false;
                if (event.getOption("use_snapshots") != null) {
                    useSnapshots = event.getOption("use_snapshots").getAsBoolean();
                }
                String currentVersion = getCurrentCobblemonVersion();
                String latestVersion = getLatestCobblemonVersionFromURL(useSnapshots);
                boolean upToDate = false;
                if (currentVersion != null && latestVersion != null) {
                    upToDate = normalizeVersion(currentVersion).equals(normalizeVersion(latestVersion));
                }
                String mcVersion = getCurrentMinecraftVersion();
                String latestRelease = getLatestCobblemonVersionFromURL(false);
                String latestSnapshot = getLatestCobblemonVersionFromURL(true);
                String reply = "Current Minecraft version: " + (mcVersion != null ? mcVersion : "Unknown") +
                        "\nCurrent Cobblemon version: " + (currentVersion != null ? currentVersion : "Unknown") +
                        "\nLatest Cobblemon release: " + (latestRelease != null ? latestRelease : "Unknown") +
                        "\nLatest Cobblemon snapshot: " + (latestSnapshot != null ? latestSnapshot : "Unknown") +
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
            // In onSlashCommandInteraction:
            case "updatecobblemon" -> {
                boolean ignoreMcVersion = false;
                boolean useSnapshots = false;
                if (event.getOption("ignore_mc_version") != null) {
                    ignoreMcVersion = event.getOption("ignore_mc_version").getAsBoolean();
                }
                if (event.getOption("use_snapshots") != null) {
                    useSnapshots = event.getOption("use_snapshots").getAsBoolean();
                }
                event.reply("Starting Cobblemon update...").setEphemeral(true).queue();
                long userId = event.getUser().getIdLong();
                boolean finalIgnoreMcVersion = ignoreMcVersion;
                boolean finalUseSnapshots = useSnapshots;
                new Thread(() -> updateCobblemon(userId, finalIgnoreMcVersion, finalUseSnapshots)).start();
            }
            case "updatespecified" -> {
                String url = event.getOption("url").getAsString();
                event.reply("Starting update from specified URL...").setEphemeral(true).queue();
                long userId = event.getUser().getIdLong();
                new Thread(() -> updateCobblemonFromUrl(userId, url)).start();
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

    public String getLatestCobblemonVersionFromURL(boolean useSnapshots) {
        String latestVersion = "unknown";
        String artefactsBase = "https://artefacts.cobblemon.com";
        String artefactsPath = useSnapshots ? "/snapshots/com/cobblemon/fabric/" : "/releases/com/cobblemon/fabric/";
        String listUrl = artefactsBase + artefactsPath;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(listUrl).openStream()))) {
            String line;
            Pattern dirPattern = Pattern.compile("href=\"\\.\\/(.+?)\\/\"");
            List<String> versionDirs = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                Matcher dirMatcher = dirPattern.matcher(line);
                while (dirMatcher.find()) {
                    versionDirs.add(dirMatcher.group(1));
                }
            }
            // Find the highest version directory
            String highestVersion = null;
            for (String dir : versionDirs) {
                // Only consider non-snapshot dirs if not using snapshots
                if (!useSnapshots && dir.contains("SNAPSHOT")) continue;
                String version = dir.split("\\+")[0];
                if (highestVersion == null || compareVersions(version, highestVersion) > 0) {
                    highestVersion = version;
                }
            }
            if (highestVersion != null) {
                latestVersion = highestVersion;
            }
        } catch (Exception e) {
            Logger.log(e);
            latestVersion = "No servers available for downloading";
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

    public void updateCobblemon(long userID, boolean ignoreMcVersion, boolean useSnapshots) {
        String latestVersion = this.getLatestCobblemonVersionFromURL(useSnapshots);
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

        // Helper class
        class FabricJar {
            String version;
            String url;
            String filename;
            String mcVersion;
        }

        // Fallback: Try Cobblemon artefacts
        if (!downloaded) {
            try {
                String artefactsBase = "https://artefacts.cobblemon.com";
                String artefactsPath = useSnapshots ? "/snapshots/com/cobblemon/fabric/" : "/releases/com/cobblemon/fabric/";
                String listUrl = artefactsBase + artefactsPath;
                Logger.log("[DEBUG] Fetching available jars from: " + listUrl);

                List<FabricJar> fabricJars = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(listUrl).openStream()))) {
                    String line;
                    Pattern jarPattern = Pattern.compile("href=\"(Cobblemon-fabric-([\\d\\.]+)\\+([\\w\\.]+)\\.jar)\"");
                    while ((line = reader.readLine()) != null) {
                        Logger.log("[DEBUG] Read line: " + line);
                        Matcher matcher = jarPattern.matcher(line);
                        while (matcher.find()) {
                            String filename = matcher.group(1);
                            String version = matcher.group(2);
                            String mcVersion = matcher.group(3);
                            String url = listUrl + filename;
                            Logger.log("[DEBUG] Matched jar: filename=" + filename + ", version=" + version + ", mcVersion=" + mcVersion + ", url=" + url);
                            FabricJar jar = new FabricJar();
                            jar.version = version;
                            jar.url = url;
                            jar.filename = filename;
                            jar.mcVersion = mcVersion;
                            fabricJars.add(jar);
                        }
                    }
                }

// After reading the root directory and before filtering by MC version
                if (fabricJars.isEmpty()) {
                    // Parse for subdirectories (version folders)
                    Pattern dirPattern = Pattern.compile("href=\"\\.\\/(.+?)\\/\"");
                    List<String> versionDirs = new ArrayList<>();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(listUrl).openStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            Matcher dirMatcher = dirPattern.matcher(line);
                            while (dirMatcher.find()) {
                                String dir = dirMatcher.group(1);
                                Logger.log("[DEBUG] Found version directory: " + dir);
                                versionDirs.add(dir);
                            }
                        }
                    }
                    // For each version directory, look for jars
                    for (String dir : versionDirs) {
                        String dirUrl = listUrl + dir + "/";
                        Logger.log("[DEBUG] Checking directory: " + dirUrl);
                        try (BufferedReader dirReader = new BufferedReader(new InputStreamReader(new URL(dirUrl).openStream()))) {
                            String line;
                            // Match both with and without build suffix
                            Pattern jarPattern = Pattern.compile("href=\"\\./(fabric-([\\d\\.]+)\\+([\\d\\.]+)(?:-[^\"/]+)?\\.jar)\"");
                            while ((line = dirReader.readLine()) != null) {
                                Logger.log("[DEBUG] [DIR] Read line: " + line);
                                Matcher jarMatcher = jarPattern.matcher(line);
                                while (jarMatcher.find()) {
                                    String filename = jarMatcher.group(1);
                                    String cobblemonVersion = jarMatcher.group(2);
                                    String mcVersion = jarMatcher.group(3);
                                    String url = dirUrl + filename;
                                    Logger.log("[DEBUG] Matched jar: filename=" + filename + ", version=" + cobblemonVersion + ", mcVersion=" + mcVersion + ", url=" + url);
                                    FabricJar jar = new FabricJar();
                                    jar.version = cobblemonVersion;
                                    jar.url = url;
                                    jar.filename = filename;
                                    jar.mcVersion = mcVersion;
                                    fabricJars.add(jar);
                                }
                        }
                        } catch (Exception e) {
                            Logger.log("[DEBUG] Failed to read directory: " + dirUrl + " - " + e.getMessage());
                        }
                    }

                }


                Logger.log("[DEBUG] Total jars found: " + fabricJars.size());

                // Filter by MC version if not ignoring
                List<FabricJar> matching = new ArrayList<>();
                if (ignoreMcVersion) {
                    matching.addAll(fabricJars);
                    Logger.log("[DEBUG] ignoreMcVersion=true, using all jars");
                } else {
                    for (FabricJar jar : fabricJars) {
                        Logger.log("[DEBUG] Checking jar mcVersion=" + jar.mcVersion + " against currentMcVersion=" + currentMcVersion);
                        if (jar.mcVersion != null && jar.mcVersion.equals(currentMcVersion)) {
                            matching.add(jar);
                        }
                    }
                }
                Logger.log("[DEBUG] Matching jars after MC version filter: " + matching.size());

                if (matching.isEmpty()) {
                    Logger.log("No fabric jar found" + (ignoreMcVersion ? "" : " for MC version " + currentMcVersion) + " in artefacts response.");
                } else {
                    Logger.log("Available Fabric jars" + (ignoreMcVersion ? "" : " for MC version " + currentMcVersion) + ":");
                    for (FabricJar jar : matching) {
                        Logger.log("[DEBUG] Filename: " + jar.filename + ", Version: " + jar.version + ", MC: " + jar.mcVersion + ", URL: " + jar.url);
                    }
                    // Find the highest version
                    FabricJar latest = matching.get(0);
                    for (FabricJar jar : matching) {
                        Logger.log("[DEBUG] Comparing versions: " + jar.version + " vs " + latest.version);
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
                        Logger.log("[DEBUG] Downloaded jar to: " + tempFile.toString());
                        downloaded = true;
                    }
                }
            } catch (Exception e) {
                Logger.log("Cobblemon artefacts download failed.");
                Logger.log("[DEBUG] Exception: " + e.getMessage());
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

    public void updateCobblemonFromUrl(long userID, String jarUrl) {
        try {
            // Step 1: Download the HTML page
            HttpURLConnection conn = (HttpURLConnection) new URL(jarUrl).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            int responseCode = conn.getResponseCode();
            String contentType = conn.getContentType();
            Logger.log("Initial URL: " + jarUrl);
            Logger.log("HTTP response code: " + responseCode);
            Logger.log("Content-Type: " + contentType);

            if (responseCode != 200 || (contentType != null && !contentType.contains("html"))) {
                this.sendDiscordDM(userID, "Failed to fetch artifact page: invalid response.");
                return;
            }

            // Step 2: Parse HTML for the raw artifact download link
            String html;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                html = sb.toString();
            }
            Pattern p = Pattern.compile("href=\"([^\"]+/artifacts/raw/[^\"]+\\.jar[^\"]*)\"");
            Matcher m = p.matcher(html);
            String rawHref = null;
            while (m.find()) {
                String candidate = m.group(1);
                if (candidate.contains("/artifacts/raw/")) {
                    rawHref = candidate;
                    break;
                }
            }
            if (rawHref == null) {
                Logger.log("Could not find raw artifact link in HTML.");
                this.sendDiscordDM(userID, "Could not find the JAR download link on the artifact page.");
                return;
            }

            // Step 3: Build the absolute URL
            URL base = new URL(jarUrl);
            String rawUrl = new URL(base, rawHref).toString();
            Logger.log("Resolved raw artifact URL: " + rawUrl);

            // Step 4: Download the JAR (only once)
            HttpURLConnection rawConn = (HttpURLConnection) new URL(rawUrl).openConnection();
            rawConn.setRequestProperty("User-Agent", "Mozilla/5.0");
            int rawCode = rawConn.getResponseCode();
            String rawType = rawConn.getContentType();
            Logger.log("Raw JAR HTTP response code: " + rawCode);
            Logger.log("Raw JAR Content-Type: " + rawType);

            if (rawCode != 200 || (rawType != null && !rawType.contains("jar") && !rawType.contains("octet-stream"))) {
                this.sendDiscordDM(userID, "Failed to download JAR: invalid response from raw artifact URL.");
                return;
            }

            String fileName = rawUrl.substring(rawUrl.lastIndexOf('/') + 1);
            int jarIndex = fileName.indexOf(".jar");
            if (jarIndex != -1) {
                fileName = fileName.substring(0, jarIndex + 4);
            }
            Path modsDir = Paths.get("mods");
            Files.createDirectories(modsDir);
            Path jarFile = modsDir.resolve(fileName);
            try (InputStream in = rawConn.getInputStream()) {
                Files.copy(in, jarFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Step 5: Validate the JAR from disk
            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile.toFile())) {
                // Validation logic (optional: check manifest, etc.)
                // Remove old jars except the new one
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "Cobblemon-fabric-*.jar")) {
                    for (Path path : stream) {
                        if (!path.getFileName().toString().equals(fileName)) {
                            Files.deleteIfExists(path);
                        }
                    }
                }
                this.sendDiscordDM(userID, "Downloaded and replaced Cobblemon with the specified jar: " + fileName);
                this.sendDiscordDM(userID, "Rebooting the Minecraft server to apply the update...");
                try { Thread.sleep(5000); } catch (InterruptedException e) { /*ignore*/ }
                MinecraftServer server = Cobblemonupdater.getServer();
                if (server != null) server.halt(true);
            } catch (Exception e) {
                Logger.log("Downloaded file is not a valid jar: " + e.getMessage());
                Files.deleteIfExists(jarFile);
                this.sendDiscordDM(userID, "Downloaded file is not a valid Cobblemon jar.");
            }
        } catch (Exception e) {
            Logger.log(e);
            this.sendDiscordDM(userID, "Failed to download or replace Cobblemon jar from the specified URL.");
        }
    }






}
