package io.github.adainish.cobblemonupdater.api.discord;

import net.dv8tion.jda.api.entities.Guild;

public interface IDiscordUser
{
    long discordID();
    String discordName(IDiscordBot bot);
    String discordTag(IDiscordBot bot);
    String discordAvatar(IDiscordBot bot);
    boolean isBot(IDiscordBot bot);
    void sendPrivateMessage(IDiscordBot bot, String message);
    void sendPrivateMessage(IDiscordBot bot, String message, String channel);
    void sendPrivateMessage(IDiscordBot bot, String message, long channel);
    void ban(Guild guild);
    void kick(Guild guild);
    void mute(Guild guild);
    void unmute(Guild guild);
    void addRole(Guild guild, String role);
    void removeRole(Guild guild, String role);
    void addRole(Guild guild, long role);
    void removeRole(Guild guild, long role);
    void removeRoles(Guild guild);
    void removeRoles(Guild guild, String... roles);
    void removeRoles(Guild guild, long... roles);
    void addRoles(Guild guild, String... roles);
    void addRoles(Guild guild, long... roles);
    void setNickname(Guild guild, String nickname);
    boolean isBanned(Guild guild);
}
