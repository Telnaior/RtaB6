package tel.discord.rtab.commands.mod;

import java.util.List;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

public class ListGuildsCommand extends Command
{
	public ListGuildsCommand()
	{
		this.name = "listguilds";
		this.help = "displays a list of guilds the bot is connected to, as well as their owners";
		this.ownerCommand = true;
		this.hidden = true;
		this.guildOnly = false;
	}
	
	@Override
	protected void execute(CommandEvent event)
	{
		List<Guild> guildList = event.getJDA().getGuilds();
		for(Guild guild : guildList)
		{
			StringBuilder output = new StringBuilder();
			output.append("**"+guild.getName()+"**\n");
			output.append("ID: "+guild.getId()+"\n");
			Member guildOwner = guild.retrieveOwner().complete();
			output.append("Owner: "+ guildOwner.getEffectiveName() + " (" + guildOwner.getUser().getName() + ")\n");
			output.append("Icon: "+guild.getIconUrl()+"\n");
			output.append("Banner: "+guild.getBannerUrl()+"\n");
			event.replyInDm(output.toString());
		}
	}
}
