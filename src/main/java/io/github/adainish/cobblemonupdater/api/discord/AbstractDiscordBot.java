package io.github.adainish.cobblemonupdater.api.discord;

import io.github.adainish.cobblemonupdater.api.Logger;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class for a Discord bot
 * Can be extended to create a custom Discord bot
 * @Author Winglet
 * @Date 11/07/2024
 */
public abstract class AbstractDiscordBot extends ListenerAdapter implements IDiscordBot {
    /**
     * Default channel to send messages to
     */
    private String defaultChannel;
    /**
     * Token to log in with
     */
    private String token;
    /**
     * JDA object
     */
    private JDA jda;
    /**
     * Server ID
     */
    private String serverID;

    @Override
    public String getDefaultChannel() {
        return this.defaultChannel;
    }

    @Override
    public void setDefaultChannel(String defaultChannel) {
        this.defaultChannel = defaultChannel;
    }

    @Override
    public String getToken() {
        return this.token;
    }

    @Override
    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void sendMessage(String message) {
        this.sendMessage(message, this.defaultChannel);
    }

    @Override
    public void sendMessage(String message, String channel) {
        broadcast(message, channel, false);
    }

    /**
     * Get a user by ID
     * @return User
     */
    @Override
    public JDA getJDA() {
        return this.jda;
    }

    /**
     * Set the JDA object
     * @param jda JDA object
     */
    @Override
    public void setJDA(JDA jda) {
        this.jda = jda;
    }

    /**
     * Get a user by ID
     * @return User
     */
    @Override
    public String generateInvite() {
        return generateInvite(this.jda);
    }

    /**
     * Generate an invite link for the bot
     * @param jda JDA object
     * @return Invite link
     */
    @Override
    public String generateInvite(JDA jda) {
        return jda.getInviteUrl(Permission.ADMINISTRATOR);
    }

    /**
     * Get the server ID
     * @return Server ID
     */
    @Override
    public String getServerID() {
        return this.serverID;
    }

    /**
     * Set the server ID
     * @param serverID Server ID to set
     */
    @Override
    public void setServerID(String serverID) {
        this.serverID = serverID;
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
            jdaBuilder.setActivity(Activity.playing("Iverium Core v1.0"));

            this.jda = jdaBuilder.build();
            jda.awaitReady();
            Logger.log("Discord bot logged in successfully!");

        } catch (Exception e) {
            Logger.log(e.getMessage());
            throw new Exception("Failed to log in");
        }

    }

    /**
     * Log out of the bot
     */
    @Override
    public void logout() {
        if (jda == null)
            return;
        Logger.log("Stopping discord bot...");
        List<Object> list = new ArrayList<>(jda.getRegisteredListeners());
        list.forEach(listener -> {
            jda.removeEventListener(listener);
        });
        jda.shutdown();
    }



    /**
     * Send a message to a user
     * @param user User to send message to
     * @param message Message to send
     */
    @Override
    public void message(IDiscordUser user, String message) {
        user.sendPrivateMessage(this, message);
    }

    /**
     * Broadcast a message to the default channel
     * @param message Message to send
     * @param pingEveryone Ping everyone in the channel
     */
    @Override
    public void broadcast(String message, boolean pingEveryone)
    {
        this.broadcast(message, this.defaultChannel, pingEveryone);
    }

    /**
     * Broadcast a message to a channel
     * @param message Message to send
     * @param channel Channel to send message to
     * @param pingEveryone Ping everyone in the channel
     */
    @Override
    public void broadcast(String message, String channel, boolean pingEveryone) {
        this.broadcast(null, message, channel, pingEveryone);
    }

    /**
     * Broadcast a message to a channel
     * @param title  Title of the message
     * @param message Message to send
     * @param channel Channel to send message to
     * @param pingEveryone Ping everyone in the channel
     */
    @Override
    public void broadcast(String title, String message, String channel, boolean pingEveryone) {
        this.broadcast(title, message, channel, pingEveryone, Color.RED);
    }

    /**
     * Broadcast a message to a channel
     * @param title Title of the message
     * @param message Message to send
     * @param channel Channel to send message to
     * @param pingEveryone Ping everyone in the channel
     * @param color Color of the message
     */
    @Override
    public void broadcast(String title, String message, String channel, boolean pingEveryone, Color color)
    {
        Guild guild = getGuild();
        if (guild == null)
        {
            Logger.log("Unable to return the configured Discord Guild to work with!");
            return;
        }
        if (channel == null || channel.isEmpty()) {
            Logger.log("No channel provided to send message to!");
            return;
        }

        TextChannel textChannel = guild.getTextChannelById(channel);

        if (textChannel == null) {
            Logger.log("A channel returned as non existent while attempting to send out a message");
            return;
        }
        EmbedBuilder embed = new EmbedBuilder();
        if (title != null && !title.isEmpty())
            embed.setTitle(title);
        if (pingEveryone)
            embed.appendDescription("@everyone\n");
        if (message != null && !message.isEmpty())
            embed.setDescription(message);
        embed.setColor(color);
        try {
            textChannel.sendMessageEmbeds(embed.build()).submit();
        } catch (Exception e)
        {
            Logger.log("Failed to send message...");
        }
    }

    /**
     * Get a custom emoji by name, if it exists
     * @param name Name of the emoji
     * @return CustomEmoji
     */
    public CustomEmoji getCustomEmoji(String name) {
        Guild guild = getGuild();
        if (guild != null) {
            return guild.getEmojis().stream().filter(emoji -> emoji.getName().equals(name)).findFirst().orElse(null);
        }
        return null;
    }

    /**
     * Get the guild object from the server ID
     * @return Guild object
     */
    @Override
    public Guild getGuild() {
        if (this.jda == null)
            return null;
        return this.jda.getGuildById(this.serverID);
    }

    public void sendDiscordDM(long userId, String message) {
        if (this.jda == null) {
            Logger.log("JDA is not initialized.");
            return;
        }

        this.jda.retrieveUserById(userId).queue(user -> {
            user.openPrivateChannel().queue(privateChannel -> {
                privateChannel.sendMessage(message).queue();
            }, failure -> {
                Logger.log("Failed to open private channel with user ID: " + userId);
            });
        }, failure -> {
            Logger.log("Failed to retrieve user with ID: " + userId);
        });
    }
}
