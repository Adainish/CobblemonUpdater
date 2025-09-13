package io.github.adainish.cobblemonupdater.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger
{
    private static final String LOG_FILE_PATH = "config/CobblemonUpdater/application.log";

    public static boolean log(String message) {
        String timestampedMessage = getTimestamp() + " - " + message;
        System.out.println(timestampedMessage);
        return appendToFile(LOG_FILE_PATH, timestampedMessage);
    }

    public static boolean log(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return log(sw.toString());
    }

    private static String getTimestamp() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }

    private static boolean appendToFile(String filePath, String text) {
        try {
            Path path = Paths.get(filePath);
            // Check if the directory exists
            if (!Files.exists(path.getParent())) {
                // If not, create the directory
                Files.createDirectories(path.getParent());
            }
            // Now write to the file
            Files.write(Paths.get(filePath), (text + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
