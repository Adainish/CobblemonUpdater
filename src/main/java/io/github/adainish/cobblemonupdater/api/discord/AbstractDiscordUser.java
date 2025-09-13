package io.github.adainish.cobblemonupdater.api.discord;

import io.github.adainish.cobblemonupdater.api.Logger;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.exceptions.HierarchyException;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract class for a Discord user
 */
public abstract class AbstractDiscordUser implements IDiscordUser {
    /**
     * Discord ID
     */
    private long discordID;
    /**
     * Minecraft UUID
     */
    private UUID minecraftUUID;
    public AbstractDiscordUser()
    {

    }

    /**
     * Constructor for a Discord user
     * @param discordID The Discord ID of the user
     */
    public AbstractDiscordUser(long discordID)
    {
        this.discordID = discordID;
    }
    @Override
    public long discordID() {
        return this.discordID;
    }

    @Override
    public String discordName(IDiscordBot bot) {
        return bot.getJDA().getUserById(this.discordID).getGlobalName();
    }

    @Override
    public String discordTag(IDiscordBot bot) {
        return bot.getJDA().getUserById(this.discordID).getGlobalName() + "#" + bot.getJDA().getUserById(this.discordID).getDiscriminator();
    }

    @Override
    public String discordAvatar(IDiscordBot bot) {
        return bot.getJDA().getUserById(this.discordID).getAvatarUrl();
    }

    @Override
    public boolean isBot(IDiscordBot bot) {
        return bot.getJDA().getUserById(this.discordID).isBot();
    }

    @Override
    public void sendPrivateMessage(IDiscordBot bot, String message) {
        bot.getJDA().getUserById(this.discordID).openPrivateChannel().queue(channel -> channel.sendMessage(message).queue());
    }

    @Override
    public void sendPrivateMessage(IDiscordBot bot, String message, String channel) {
        bot.getJDA().getGuildById(bot.getServerID()).getTextChannelById(channel).sendMessage(message).queue();
    }

    @Override
    public void sendPrivateMessage(IDiscordBot bot, String message, long channel) {
        bot.getJDA().getGuildById(bot.getServerID()).getTextChannelById(channel).sendMessage(message).queue();
    }

    @Override
    public void ban(Guild guild) {
        UserSnowflake user = UserSnowflake.fromId(this.discordID);
        guild.ban(user, 31, TimeUnit.DAYS).queue();
    }

    @Override
    public void kick(Guild guild) {
        UserSnowflake user = UserSnowflake.fromId(this.discordID);
        guild.kick(user).queue();
    }

    @Override
    public void mute(Guild guild) {
        //TODO: Implement mute
    }

    @Override
    public void unmute(Guild guild) {
        //TODO: Implement unmute
    }

    @Override
    public void addRole(Guild guild, String role) {
        UserSnowflake user = UserSnowflake.fromId(this.discordID);
        Role actualRole = guild.getRolesByName(role, true).get(0);
        if (actualRole == null)
            return;
        guild.addRoleToMember(user, actualRole).queue();
    }

    @Override
    public void removeRole(Guild guild, String role) {
        UserSnowflake user = UserSnowflake.fromId(this.discordID);
        Role actualRole = guild.getRolesByName(role, true).get(0);
        if (actualRole == null)
            return;
        guild.removeRoleFromMember(user, actualRole).queue();
    }

    @Override
    public void addRole(Guild guild, long role) {
        UserSnowflake user = UserSnowflake.fromId(this.discordID);
        Role actualRole = guild.getRoleById(role);
        if (actualRole == null)
            return;
        guild.addRoleToMember(user, actualRole).queue();
    }

    @Override
    public void removeRole(Guild guild, long role) {
        UserSnowflake user = UserSnowflake.fromId(this.discordID);
        Role actualRole = guild.getRoleById(role);
        if (actualRole == null)
            return;
        guild.removeRoleFromMember(user, actualRole).queue();
    }

    @Override
    public void removeRoles(Guild guild) {
        try {
            UserSnowflake user = UserSnowflake.fromId(this.discordID);
            guild.retrieveMemberById(this.discordID).queue(member -> {
                member.getRoles().forEach(role -> {
                    guild.removeRoleFromMember(user, role).queue();
                });
            });
        } catch (HierarchyException e) {
            Logger.log(e);
            Logger.log("Or to translate, the discord bot can not modify roles higher than itself.");
        }
    }

    @Override
    public void removeRoles(Guild guild, String... roles) {
        UserSnowflake user = UserSnowflake.fromId(this.discordID);
        guild.retrieveMemberById(this.discordID).queue(member -> {
            member.getRoles().forEach(role -> {
                Arrays.stream(roles).filter(roleName -> role.getName().equalsIgnoreCase(roleName)).forEachOrdered(roleName -> guild.removeRoleFromMember(user, role).queue());
            });
        });
    }

    @Override
    public void removeRoles(Guild guild, long... roles) {
        UserSnowflake user = UserSnowflake.fromId(this.discordID);
        guild.retrieveMemberById(this.discordID).queue(member -> {
            member.getRoles().forEach(role -> {
                Arrays.stream(roles).filter(roleID -> role.getIdLong() == roleID).forEachOrdered(roleID -> guild.removeRoleFromMember(user, role).queue());
            });
        });
    }

    @Override
    public void addRoles(Guild guild, String... roles) {
        UserSnowflake user = UserSnowflake.fromId(this.discordID);
        guild.retrieveMemberById(this.discordID).queue(member -> {
            Arrays.stream(roles).map(roleName -> guild.getRolesByName(roleName, true).get(0)).filter(Objects::nonNull).forEachOrdered(role -> guild.addRoleToMember(user, role).queue());
        });
    }

    @Override
    public void addRoles(Guild guild, long... roles) {
        UserSnowflake user = UserSnowflake.fromId(this.discordID);
        guild.retrieveMemberById(this.discordID).queue(member -> {
            Arrays.stream(roles).mapToObj(guild::getRoleById).filter(Objects::nonNull).forEachOrdered(role -> guild.addRoleToMember(user, role).queue());
        });
    }

    @Override
    public void setNickname(Guild guild, String nickname) {
        Member member = guild.getMember(UserSnowflake.fromId(this.discordID));
        if (member != null)
            guild.modifyNickname(member, nickname).queue();
    }

    @Override
    public boolean isBanned(Guild guild) {
        AtomicBoolean isBanned = new AtomicBoolean(false);
        guild.retrieveBanList().queue(banList -> {
            if (banList.stream().anyMatch(ban -> ban.getUser().getIdLong() == this.discordID)) isBanned.set(true);
        });
        return isBanned.get();
    }
}
