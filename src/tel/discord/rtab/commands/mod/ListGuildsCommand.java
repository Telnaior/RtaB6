package tel.discord.rtab.commands.mod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
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
		int totalGuildCount = 0;
		List<Guild> guildList = event.getJDA().getGuilds();
		for(Guild guild : guildList)
		{
			totalGuildCount ++;
			StringBuilder output = new StringBuilder();
			output.append("**"+guild.getName()+"**\n");
			output.append("ID: "+guild.getId()+"\n");
			Member guildOwner = guild.retrieveOwner().complete();
			output.append("Owner: "+ guildOwner.getEffectiveName() + " (" + guildOwner.getUser().getName() + ")\n");
			output.append("Icon: "+guild.getIconUrl()+"\n");
			output.append("Banner: "+guild.getBannerUrl()+"\n");
			try
			{
				int enabledChannels = 0;
				List<String> list = Files.readAllLines(Paths.get("guilds","guild"+guild.getId()+".csv"));
				for(String channelString : list)
				{
					String[] record = channelString.split("#");
					if(record[1].equalsIgnoreCase("enabled"))
						enabledChannels++;
				}
				output.append("Enabled Channels: ").append(enabledChannels).append("\n");
				FileTime modifiedTime = (FileTime)Files.getAttribute(Paths.get("guilds","guild"+guild.getId()+".csv"),"lastModifiedTime");
				long modifiedTimeAgo = System.currentTimeMillis() - modifiedTime.toMillis();
				output.append("Last Modified: ").append(modifiedTimeAgo / 86_400_000).append(" days ago\n");
			}
			catch(IOException e) { } //I just can't bring myself to care
			event.replyInDm(output.toString());
		}
		event.replyInDm("Currently in " + totalGuildCount + " total guilds.");
	}
}
