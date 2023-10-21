package tel.discord.rtab.commands.mod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.entities.Guild;

public class CleanUpChannelsCommand extends Command
{
	public CleanUpChannelsCommand()
	{
		this.name = "cleanupchannels";
		this.help = "disables game channels the bot currently doesn't have permissions to access";
		this.ownerCommand = true;
		this.hidden = true;
		this.guildOnly = false;
	}
	
	@Override
	protected void execute(CommandEvent event)
	{
		int channelsCleanedUp = 0;
		List<Guild> guildList = event.getJDA().getGuilds();
		for(Guild guild : guildList)
		{
			try
			{
				Path file = Paths.get("guilds","guild"+guild.getId()+".csv");
				List<String> list = Files.readAllLines(file);
				for(int i=0; i<list.size(); i++)
				{
					String[] record = list.get(i).split("#");
					//If getting the channel returns null, our bot can't access it and we want it disabled
					if(record[1].equalsIgnoreCase("enabled") && guild.getTextChannelById(record[0]) == null)
					{
						//Disable the channel 
						record[1] = "disabled";
						//and save the settings file
						StringBuilder fullLine = new StringBuilder();
						for(String next : record)
						{
							fullLine.append("#");
							fullLine.append(next);
						}
						//Remove the opening #
						fullLine.deleteCharAt(0);
						list.set(i, fullLine.toString());
						Path oldFile = Files.move(file, file.resolveSibling("guild"+guild.getId()+"old.csv"));
						Files.write(file, list);
						Files.delete(oldFile);
					}
				}
			}
			catch(IOException e) { } //I just can't bring myself to care
		}
		event.replyInDm(channelsCleanedUp + " channels cleaned up.");
	}
}
