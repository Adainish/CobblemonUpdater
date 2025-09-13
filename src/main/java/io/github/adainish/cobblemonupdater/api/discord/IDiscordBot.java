package io.github.adainish.cobblemonupdater.api.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

import java.awt.*;

public interface IDiscordBot
{
    String getDefaultChannel();
    void setDefaultChannel(String defaultChannel);
    String getToken();
    void setToken(String token);
    void start();
    void stop();
    void sendMessage(String message);
    void sendMessage(String message, String channel);
    JDA getJDA();
    void setJDA(JDA jda);
    String generateInvite();
    String generateInvite(JDA jda);
    String getServerID();
    void setServerID(String serverID);
    void login(String args) throws Exception;
    void logout();
    void message(IDiscordUser user, String message);
    void broadcast(String message, boolean pingEveryone);
    void broadcast(String message, String channel, boolean pingEveryone);
    void broadcast(String title, String message, String channel, boolean pingEveryone);
    void broadcast(String title, String message, String channel, boolean pingEveryone, Color color);
    Guild getGuild();
}
