package io.github.adainish.cobblemonupdater.api.discord;

import net.dv8tion.jda.api.entities.Guild;

public class DiscordUser extends AbstractDiscordUser implements IDiscordUser
{
    public DiscordUser()
    {

    }

    /**
     * Constructor for a Discord user
     * @param discordID The Discord ID of the user
     */
    public DiscordUser(long discordID)
    {
        super(discordID);
    }

    /**
     * Check if the player is linked to a Minecraft account
     * @param guild The guild to check the role in
     * @param roleID The role ID to check
     * @return Whether the player has the role
     */
    public boolean hasRole(Guild guild, Long roleID) {
        return guild.getMemberById(this.discordID()).getRoles().contains(guild.getRoleById(roleID));
    }
}
